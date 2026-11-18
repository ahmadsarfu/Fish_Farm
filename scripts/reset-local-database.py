#!/usr/bin/env python3
"""Back up and rebuild only the local fish_farm_db for a fresh walkthrough."""
import argparse
import datetime
import getpass
import os
from pathlib import Path
import shutil
import shlex
import socket
import subprocess
import sys
import tempfile

DATABASE = 'fish_farm_db'
ROOT = Path(__file__).resolve().parent.parent


def option(value):
    return '"' + str(value).replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n').replace('\r', '\\r') + '"'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--host', choices=['localhost', '127.0.0.1', '::1'], default='localhost')
    parser.add_argument('--port', type=int, default=3306)
    parser.add_argument('--user', default='root')
    parser.add_argument('--api-port', type=int, default=8080)
    args = parser.parse_args()
    if not (1 <= args.port <= 65535 and 1 <= args.api_port <= 65535):
        parser.error('Ports must be between 1 and 65535')
    for binary in ['mysql', 'mysqldump']:
        if not shutil.which(binary):
            raise RuntimeError(f'{binary} is required. Install the MySQL client tools first.')
    for host in ['127.0.0.1', '::1']:
        try:
            connection = socket.create_connection((host, args.api_port), timeout=1)
        except OSError:
            continue
        connection.close()
        raise RuntimeError(f'Stop the backend listening on port {args.api_port} before resetting its database.')
    scripts = [ROOT / 'docs/sql/schema-full.sql', ROOT / 'docs/sql/platform-onboarding-upgrade.sql']
    contents = [path.read_text() for path in scripts]
    password = getpass.getpass(f'MySQL password for {args.user}@{args.host}: ')
    local = ROOT / '.local'
    local.mkdir(mode=0o700, exist_ok=True)
    backups = local / 'backups'
    backups.mkdir(mode=0o700, exist_ok=True)
    backup = None
    try:
        with tempfile.TemporaryDirectory(prefix='database-reset-', dir=local) as private:
            config = Path(private) / 'client.cnf'
            settings = [('user', args.user), ('password', password), ('host', args.host), ('port', args.port)]
            if args.host != 'localhost' or args.port != 3306:
                settings.append(('protocol', 'TCP'))
            config.write_text('[client]\n' + '\n'.join(f'{key}={option(value)}' for key, value in settings) + '\n')
            config.chmod(0o600)
            password = None
            client = ['mysql', f'--defaults-file={config}', '--batch', '--skip-column-names']

            def query(text, database=True):
                command = client + ([DATABASE] if database else [])
                result = subprocess.run(command, input=text, text=True, capture_output=True)
                if result.returncode:
                    raise RuntimeError(result.stderr.strip() or 'MySQL command failed')
                return result.stdout.strip()

            version = query('SELECT VERSION();')
            if 'mariadb' in version.lower() or int(version.split('.')[0]) < 8:
                raise RuntimeError('This schema requires MySQL 8 or newer.')
            query('SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE();')
            stamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
            with tempfile.NamedTemporaryFile(prefix=f'fish_farm_db_{stamp}_', suffix='.sql.partial', dir=backups, delete=False) as handle:
                partial = Path(handle.name)
                result = subprocess.run([
                    'mysqldump', f'--defaults-file={config}', '--single-transaction', '--no-tablespaces',
                    '--routines', '--events', '--triggers', '--set-gtid-purged=OFF', '--add-drop-database',
                    '--databases', DATABASE,
                ], stdout=handle, stderr=subprocess.PIPE, stdin=subprocess.DEVNULL)
                handle.flush()
                os.fsync(handle.fileno())
            if result.returncode or partial.stat().st_size == 0:
                raise RuntimeError('Backup failed. The database was not cleared. ' + result.stderr.decode(errors='replace').strip())
            backup = partial.with_suffix('')
            partial.rename(backup)
            print(f'Backup saved: {backup}', flush=True)
            print('Rebuilding only fish_farm_db. Old accounts and farm records will be removed.', flush=True)
            query('DROP DATABASE `fish_farm_db`; CREATE DATABASE `fish_farm_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;', database=False)
            for text in contents:
                query(text)
            columns = query("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND ((table_name='app_accounts' AND column_name IN ('email','mobile','platform_admin')) OR (table_name='farms' AND column_name IN ('plan_name','subscription_status','subscription_ends_on','managed_subscription','pond_limit','user_limit')));")
            if columns != '9':
                raise RuntimeError('Schema verification failed')
            if query('SELECT (SELECT COUNT(*) FROM app_accounts)+(SELECT COUNT(*) FROM farms);') != '0':
                raise RuntimeError('The new installation unexpectedly contains account or farm records')
            print('Fresh schema verified. No default login or demo customer has been created.')
            print('Next: bash scripts/setup-platform-admin.sh')
            print('Then open http://localhost:4174/admin/login and add your first customer.')
    except Exception:
        if backup:
            print(f'Reset did not complete. Keep the API stopped. Your backup is: {backup}', file=sys.stderr)
            restore = ['mysql', f'--host={args.host}', f'--port={args.port}', f'--user={args.user}', '-p']
            if args.host != 'localhost' or args.port != 3306:
                restore.append('--protocol=TCP')
            print(f'Restore with: {shlex.join(restore)} < {shlex.quote(str(backup))}', file=sys.stderr)
        raise


if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, OSError, ValueError) as failure:
        print(str(failure), file=sys.stderr)
        sys.exit(1)
    except (KeyboardInterrupt, EOFError):
        print('\nReset cancelled. Check the backup message above before restarting the API.', file=sys.stderr)
        sys.exit(130)

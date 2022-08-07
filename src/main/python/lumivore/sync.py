import argparse
import hashlib
import logging
import os
import sqlite3
import sys

from lumivore.dao import LumivoreDao
from lumivore import fs


def create_arg_parser():
    p = argparse.ArgumentParser(
        prog='sync',
        description='Sync file hashes to the database',
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
        add_help=True)
    p.add_argument('--db', help='sqlite db file path', default=os.path.join(os.getcwd(), 'photos.db'))
    p.add_argument('--dry', help='dry run', default=False, action='store_const', const=True)
    p.add_argument('--not-ext', help='extensions to NOT import', nargs='*', default=[])
    return p


def ensure(path):
    if not os.path.exists(path):
        print("No such path: ", path, file=sys.stderr)
        sys.exit(-1)


def hash_file(file_path):
    sha = hashlib.sha1()
    with open(file_path, 'rb') as f:
        for chunk in iter(lambda: f.read(4096), b""):
            sha.update(chunk)
    return sha.hexdigest().upper()


def sync(path, db, extensions, hash_by_path, log):
    log.info("syncing %s", path)
    updated = False

    for path in fs.eachfile(path, extensions):
        if path in hash_by_path:
            log.info('seen|%s', path)
            continue
        hash_hex = hash_file(path)
        log.info('unseen|%s|%s', path, hash_hex)
        db.insert_sync(path, hash_hex)
        updated = True

    if updated:
        db.commit()


def main(args):
    parser = create_arg_parser()
    options = parser.parse_args(args)
    log = logging.getLogger('lumivore.sync')
    log.setLevel(logging.INFO)
    handler = logging.StreamHandler()
    handler.setFormatter(logging.Formatter('%(asctime)-15s|%(levelname)s|%(message)s'))
    log.addHandler(handler)
    ensure(options.db)

    with LumivoreDao(sqlite3.connect(options.db), log, options.dry) as db:
        extensions = db.get_extensions()
        extensions = [e for e in extensions if e not in options.not_ext]
        watched_paths = db.get_watched_paths()
        hash_by_path = db.get_hash_by_path()
        for path in watched_paths:
            sync(path, db, extensions, hash_by_path, log)


if __name__ == '__main__':
    main(sys.argv[1:])

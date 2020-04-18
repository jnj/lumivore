import datetime


class LumivoreDao:
    def __init__(self, conn, log, dryrun):
        self.dryrun = dryrun
        self.conn = conn
        self.log = log

    def get_extensions(self):
        cursor = self.conn.execute('select extension from extensions')
        return set([r[0].lower() for r in cursor.fetchall()])

    def get_watched_paths(self):
        cursor = self.conn.execute('select path from indexed_paths')
        return [r[0] for r in cursor.fetchall()]

    def get_hash_by_path(self):
        cursor = self.conn.execute('select path, sha1 from syncs')
        results = {}

        for r in cursor:
            path, hashstr = r
            results[path] = hashstr

        return results

    def insert_sync(self, path, hash_str):
        if self.dryrun:
            return
        now = datetime.datetime.now()
        ts = int(now.timestamp() * 1000)
        self.conn.execute('insert into syncs values (?, ?, ?)', (path, hash_str, ts))

    def commit(self):
        if self.dryrun:
            return
        self.conn.commit()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()

    def close(self):
        self.conn.close()

"""Recover OAI-PMH repository symbols for previously published DataCite DOIs."""

import argparse
import json
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import quote, urlsplit
from urllib.request import Request, urlopen
from uuid import UUID

import pymysql


def lookup(api, doi, landing_pages):
    request = Request(api + "/dois/" + quote(doi, safe=""), headers={"Accept": "application/vnd.api+json"})
    try:
        with urlopen(request, timeout=15) as response:
            data = json.load(response)["data"]
    except HTTPError as exc:
        if exc.code == 404:
            return None
        raise

    attributes = data["attributes"]
    client = (data.get("relationships", {}).get("client") or {}).get("data") or {}
    symbol = client.get("id")
    url = attributes.get("url")
    if (
        data.get("type") == "dois"
        and attributes.get("doi", "").casefold() == doi.casefold()
        and attributes.get("state") == "findable"
        and isinstance(url, str) and url.rstrip("/") in landing_pages
        and client.get("type") == "clients"
        and isinstance(symbol, str) and symbol and symbol == symbol.strip() and len(symbol.upper()) <= 255
    ):
        return symbol.upper()
    return None


def run(connection, args):
    api = "https://api.test.datacite.org" if args.test else "https://api.datacite.org"
    cursor_id = str(args.after) if args.after else ""
    with connection.cursor() as cursor:
        cursor.execute("""
            SELECT id, doi FROM mdb_identifiers
            WHERE status = 'PUBLISHED' AND doi IS NOT NULL AND doi <> ''
              AND datacite_repository IS NULL AND id > %s
            ORDER BY id LIMIT %s
        """, (cursor_id, args.limit))
        rows = cursor.fetchall()
        matched = updated = 0
        for pid, doi in rows:
            urls = {base.rstrip("/") + "/pid/" + pid for base in args.public_url}
            try:
                symbol = lookup(api, doi, urls)
            except (HTTPError, URLError, TimeoutError, ValueError, KeyError) as exc:
                print(f"Registry lookup for {pid} failed ({exc}). Last completed ID: {cursor_id or '(first page)'}",
                      file=sys.stderr)
                return 1
            if symbol:
                matched += 1
                if args.apply:
                    cursor.execute("""
                        UPDATE mdb_identifiers SET datacite_repository = %s, last_modified = CURRENT_TIMESTAMP
                        WHERE id = %s AND doi = %s AND status = 'PUBLISHED' AND datacite_repository IS NULL
                    """, (symbol, pid, doi))
                    updated += cursor.rowcount
            else:
                print(f"{pid} has no matching public DataCite registration for this landing page")
            cursor_id = pid
    print(f"Checked {len(rows)}. Matched {matched}. Updated {updated}.")
    if rows:
        print(f"Last checked ID: {cursor_id}")
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--defaults-file", required=True, help="MariaDB [client] option file")
    parser.add_argument("--database", default="dbrepo")
    parser.add_argument("--public-url", action="append", required=True,
                        help="DBRepo public website URL used for deposits; repeat for former URLs")
    parser.add_argument("--limit", type=int, default=100)
    parser.add_argument("--after", type=UUID, help="continue after the last checked ID")
    parser.add_argument("--test", action="store_true", help="consult the DataCite test registry")
    parser.add_argument("--apply", action="store_true", help="write verified repository IDs")
    args = parser.parse_args()
    if not 1 <= args.limit <= 500:
        parser.error("--limit must be between 1 and 500")
    for url in args.public_url:
        parts = urlsplit(url)
        if parts.scheme not in {"https", "http"} or not parts.netloc or parts.query or parts.fragment:
            parser.error("--public-url must be an absolute website URL")
    with pymysql.connect(read_default_file=args.defaults_file, database=args.database,
                         charset="utf8mb4", autocommit=True) as connection:
        return run(connection, args)


if __name__ == "__main__":
    sys.exit(main())

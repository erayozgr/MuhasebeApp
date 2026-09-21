"""Run with Python's standard library; no live database is used."""
import re
import sqlite3
from pathlib import Path

root = Path(__file__).resolve().parents[2]
schema_dir = root / "src/commonMain/sqldelight/com/eray/muhasebeapp/database"
schema = (schema_dir / "AppDatabase.sq").read_text(encoding="utf-8")
migration = (schema_dir / "1.sqm").read_text(encoding="utf-8")
db = sqlite3.connect(":memory:")
tables = {"UrunEntity": "stokAdedi", "SatisKalemi": "adet", "AlisKalemi": "adet", "StokHareketi": "miktar"}
before = {}
for name, amount in tables.items():
    ddl = re.search(rf"CREATE TABLE {name} \([\s\S]*?\);", schema).group()
    db.executescript(ddl.replace(f"{amount} REAL", f"{amount} INTEGER"))
    columns = db.execute(f"PRAGMA table_info({name})").fetchall()
    values = ["example" if c[2] == "TEXT" else 17 if c[1] == "id" else 2 for c in columns]
    db.execute(f"INSERT INTO {name} VALUES ({','.join('?' for _ in columns)})", values)
    before[name] = db.execute(f"SELECT * FROM {name}").fetchall()
db.executescript(migration)
for name, amount in tables.items():
    assert db.execute(f"SELECT * FROM {name}").fetchall() == before[name], name
    db.execute(f"UPDATE {name} SET {amount} = ?", (1.25,))
    assert db.execute(f"SELECT {amount}, typeof({amount}) FROM {name}").fetchone() == (1.25, "real"), name
    assert db.execute(f"SELECT id FROM {name}").fetchone()[0] == 17
print("PASS: four migrated tables retain records/IDs and store fractional quantities")

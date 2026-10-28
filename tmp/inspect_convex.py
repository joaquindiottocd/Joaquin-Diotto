import os
import re

files = [
    "/tmp/Auth-iWgXIpKg.js",
    "/tmp/Dashboard-CdkGY3i6.js",
    "/tmp/Clients-dNa0UqFl.js",
    "/tmp/ClientDetail-xC6H9zHK.js",
    "/tmp/Users-CA-Oa1YL.js",
    "/tmp/TransferAccounts-Dpg701eT.js",
    "/tmp/Holidays-D0X92aOl.js",
]

for file in files:
    print("=" * 40)
    print("FILE:", file)
    with open(file, "r") as f:
        content = f.read()
    # In Convex, api object is often generated like `api.clients.list` or similar, or `anyApi.clients...`
    # Let's search for function names, table names, or identifiers
    matches = re.findall(r"([a-zA-Z0-9_$]+)\.([a-zA-Z0-9_$]+)", content)
    # Check words related to payments, clients, debts, loans, cobranzas, etc.
    keywords = ["cliente", "cobro", "pago", "cuota", "prestamo", "usuario", "cuenta", "feriado", "dashboard", "metric", "plan"]
    found_keywords = set()
    for kw in keywords:
        if kw in content.lower():
            found_keywords.add(kw)
    print("Keywords found:", found_keywords)

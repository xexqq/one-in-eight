# Скачивает реальную статистику Всемирного банка и пишет app/src/main/assets/data.json
import json, os, urllib.request as u

B = "https://api.worldbank.org/v2/"

def get(p):
    return json.load(u.urlopen(B + p, timeout=60))

countries = {c["id"]: c for c in get("country?format=json&per_page=400")[1]
             if c["region"]["id"] != "NA"}
IND = ["SP.POP.TOTL", "SP.DYN.CBRT.IN", "SP.DYN.LE00.MA.IN", "SP.DYN.LE00.FE.IN",
       "SP.DYN.IMRT.IN", "SH.DYN.MORT", "NY.GDP.PCAP.PP.CD", "SI.POV.GINI",
       "SE.SCH.LIFE", "SH.STA.MMRT", "VC.IHR.PSRC.P5", "SH.DYN.AIDS.ZS",
       "SP.URB.TOTL.IN.ZS", "SL.AGR.EMPL.ZS", "SE.TER.ENRR", "IT.NET.USER.ZS", "SP.DYN.TFRT.IN"]
d = {}
for i, k in enumerate(IND):
    rows = get(f"country/all/indicator/{k}?format=json&per_page=20000&mrnev=1")[1] or []
    for r in rows:
        if r["value"] is not None:
            d.setdefault(r["countryiso3code"], {})[i] = r["value"]

def ll(x):
    try:
        return float(x)
    except Exception:
        return None

out = []
for code, c in countries.items():
    v = d.get(code, {})
    if 0 not in v or 1 not in v:
        continue
    out.append([c["name"], round(v[1] * v[0] / 1e6), round(v[0] / 1e6, 2)]
               + [v.get(i) for i in range(2, 17)] + [ll(c["latitude"]), ll(c["longitude"])])

path = "app/src/main/assets/data.json"
os.makedirs(os.path.dirname(path), exist_ok=True)
open(path, "w", encoding="utf-8").write(json.dumps(out, ensure_ascii=False))
print("countries:", len(out))

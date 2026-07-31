"""
Extract the `assets/` tree out of mod jars, whole, so models can be previewed offline.

Extract *whole*, not file by file. Pulling out only the textures a model happens to name
is what left the previewer unable to resolve Create's parents and cost two rounds of grid
work shipped blind.

`unzip` with a wildcard is not reliable here - on MSYS it has produced directory entries
and no files even with MSYS2_ARG_CONV_EXCL set. Python's zipfile is.

    python tools/extract_assets.py <jar-or-directory> [...]

Everything lands in tools/extracted/assets/<namespace>/..., which .gitignore already
excludes. Point it at the jars pinned by the pack, so what you preview against is what
players will run.
"""
import os
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
DEST = os.path.join(HERE, "extracted")


def jars(paths):
    for p in paths:
        if os.path.isdir(p):
            for name in sorted(os.listdir(p)):
                if name.endswith(".jar"):
                    yield os.path.join(p, name)
        elif p.endswith(".jar"):
            yield p
        else:
            print(f"skipping {p}: not a jar or directory")


def extract(jar):
    with zipfile.ZipFile(jar) as z:
        members = [n for n in z.namelist()
                   if n.startswith("assets/") and not n.endswith("/")]
        for n in members:
            target = os.path.join(DEST, n)
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with z.open(n) as src, open(target, "wb") as out:
                out.write(src.read())
    print(f"{os.path.basename(jar)}: {len(members)} files")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    for jar in jars(sys.argv[1:]):
        extract(jar)
    print("extracted into", DEST)

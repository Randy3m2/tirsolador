"""Convert preserved Carbon SVG logos to transparent Android PNG resources.

Requires CairoSVG (python -m pip install cairosvg).
"""
from pathlib import Path
import cairosvg

root = Path(__file__).resolve().parent.parent
output = root / "app/src/main/res/drawable-nodpi"
output.mkdir(parents=True, exist_ok=True)
for source in sorted((root / "design/logos").glob("*.svg")):
    # These pinned upstream files use internal SVG namespace entities.
    cairosvg.svg2png(url=str(source), write_to=str(output / f"logo_{source.stem}.png"), output_width=640, unsafe=True)

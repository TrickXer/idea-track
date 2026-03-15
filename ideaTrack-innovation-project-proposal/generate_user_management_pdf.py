from pathlib import Path
import re

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.pdfbase.pdfmetrics import stringWidth
from reportlab.platypus import (
    HRFlowable,
    PageBreak,
    Paragraph,
    Preformatted,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)


TITLE_TEXT = "System Architecture and Technical Documentation - User Management Module"
ACCENT = colors.HexColor("#1F4E79")
ACCENT_SOFT = colors.HexColor("#EAF2F8")
TEXT = colors.HexColor("#22313F")
MUTED = colors.HexColor("#5D6D7E")
RULE = colors.HexColor("#D6E2EA")
CODE_BG = colors.HexColor("#F7F9FB")


def inline_markdown_to_html(text: str) -> str:
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = re.sub(r"`([^`]+)`", r'<font name="Courier">\1</font>', text)
    text = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", text)
    text = re.sub(r"\*([^*]+)\*", r"<i>\1</i>", text)
    return text


def make_code_block(code_lines: list[str], code_style: ParagraphStyle):
    code = Preformatted("\n".join(code_lines), code_style)
    block = Table([[code]], colWidths=[16.8 * cm])
    block.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), CODE_BG),
                ("BOX", (0, 0), (-1, -1), 0.5, RULE),
                ("LEFTPADDING", (0, 0), (-1, -1), 8),
                ("RIGHTPADDING", (0, 0), (-1, -1), 8),
                ("TOPPADDING", (0, 0), (-1, -1), 6),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
            ]
        )
    )
    return block


def parse_markdown_to_story(md_text: str):
    styles = getSampleStyleSheet()
    body = styles["BodyText"]
    body.fontName = "Helvetica"
    body.fontSize = 10.2
    body.leading = 15
    body.textColor = TEXT
    body.spaceAfter = 4

    h1 = ParagraphStyle(
        "H1",
        parent=styles["Heading1"],
        fontName="Helvetica-Bold",
        fontSize=20,
        leading=24,
        textColor=ACCENT,
        spaceBefore=6,
        spaceAfter=10,
    )
    h2 = ParagraphStyle(
        "H2",
        parent=styles["Heading2"],
        fontName="Helvetica-Bold",
        fontSize=14.5,
        leading=19,
        textColor=ACCENT,
        backColor=ACCENT_SOFT,
        borderPadding=(4, 6, 4),
        spaceBefore=10,
        spaceAfter=6,
    )
    h3 = ParagraphStyle(
        "H3",
        parent=styles["Heading3"],
        fontName="Helvetica-Bold",
        fontSize=12,
        leading=16,
        textColor=TEXT,
        spaceBefore=8,
        spaceAfter=4,
    )
    title_style = ParagraphStyle(
        "TitlePageTitle",
        parent=styles["Title"],
        fontName="Helvetica-Bold",
        fontSize=24,
        leading=30,
        alignment=TA_CENTER,
        textColor=ACCENT,
        spaceAfter=10,
    )
    subtitle_style = ParagraphStyle(
        "TitlePageSubtitle",
        parent=body,
        fontName="Helvetica",
        fontSize=11,
        leading=16,
        alignment=TA_CENTER,
        textColor=MUTED,
        spaceAfter=8,
    )
    bullet_style = ParagraphStyle(
        "Bullet",
        parent=body,
        leftIndent=14,
        firstLineIndent=-10,
        bulletIndent=0,
        spaceAfter=3,
    )
    numbered_style = ParagraphStyle(
        "Numbered",
        parent=body,
        leftIndent=14,
        firstLineIndent=-10,
        spaceAfter=3,
    )
    code_style = ParagraphStyle(
        "CodeBlock",
        parent=styles["Code"],
        fontName="Courier",
        fontSize=8.5,
        leading=11,
        textColor=TEXT,
    )

    story = []
    in_code_block = False
    code_lines = []
    title_consumed = False

    story.append(Spacer(1, 2.2 * cm))
    story.append(Paragraph(TITLE_TEXT, title_style))
    story.append(
        Paragraph(
            "Client-friendly technical workflow document for presentation, review, and interview explanation.",
            subtitle_style,
        )
    )
    story.append(HRFlowable(width="70%", thickness=1, color=RULE, hAlign="CENTER"))
    story.append(Spacer(1, 0.6 * cm))

    for raw_line in md_text.splitlines():
        line = raw_line.rstrip("\n")

        if line.strip().startswith("```"):
            if in_code_block:
                story.append(make_code_block(code_lines, code_style))
                story.append(Spacer(1, 0.2 * cm))
                code_lines = []
                in_code_block = False
            else:
                in_code_block = True
            continue

        if in_code_block:
            code_lines.append(line)
            continue

        if not line.strip():
            story.append(Spacer(1, 0.15 * cm))
            continue

        if line.startswith("# "):
            if not title_consumed:
                title_consumed = True
                story.append(PageBreak())
                continue
            story.append(Paragraph(inline_markdown_to_html(line[2:].strip()), h1))
            continue

        if line.startswith("## "):
            story.append(Paragraph(inline_markdown_to_html(line[3:].strip()), h2))
            continue

        if line.startswith("### "):
            story.append(Paragraph(inline_markdown_to_html(line[4:].strip()), h3))
            continue

        if line.startswith("- "):
            story.append(Paragraph("• " + inline_markdown_to_html(line[2:].strip()), bullet_style))
            continue

        if re.match(r"^\d+\.\s", line):
            story.append(Paragraph(inline_markdown_to_html(line), numbered_style))
            continue

        if line.strip() == "---":
            story.append(Spacer(1, 0.1 * cm))
            story.append(HRFlowable(width="100%", thickness=0.5, color=RULE))
            story.append(Spacer(1, 0.15 * cm))
            continue

        story.append(Paragraph(inline_markdown_to_html(line), body))

    if code_lines:
        story.append(make_code_block(code_lines, code_style))

    return story


def draw_cover(canvas, doc):
    canvas.saveState()
    width, height = A4
    canvas.setFillColor(ACCENT)
    canvas.rect(0, height - 3.4 * cm, width, 3.4 * cm, fill=1, stroke=0)
    canvas.setFillColor(colors.white)
    canvas.setFont("Helvetica-Bold", 11)
    canvas.drawString(1.6 * cm, height - 1.6 * cm, "IdeaTrack Documentation")
    canvas.setFillColor(colors.HexColor("#DCEAF6"))
    canvas.rect(1.5 * cm, 2.0 * cm, width - 3.0 * cm, 0.5, fill=1, stroke=0)
    canvas.restoreState()


def draw_page_chrome(canvas, doc):
    canvas.saveState()
    width, height = A4
    canvas.setStrokeColor(RULE)
    canvas.setLineWidth(0.6)
    canvas.line(1.5 * cm, height - 1.1 * cm, width - 1.5 * cm, height - 1.1 * cm)
    canvas.setFont("Helvetica", 8.5)
    canvas.setFillColor(MUTED)
    canvas.drawString(1.6 * cm, height - 0.85 * cm, "IdeaTrack | User Management Module")
    page_label = f"Page {doc.page}"
    canvas.drawRightString(width - 1.6 * cm, 0.9 * cm, page_label)
    canvas.line(1.5 * cm, 1.25 * cm, width - 1.5 * cm, 1.25 * cm)
    canvas.restoreState()


def main():
    root = Path(__file__).resolve().parent
    md_path = root / "System_Architecture_and_Technical_Documentation_User_Management_Module.md"
    pdf_path = root / "System Architecture and Technical Documentation - User Management Module.pdf"

    md_text = md_path.read_text(encoding="utf-8")

    doc = SimpleDocTemplate(
        str(pdf_path),
        pagesize=A4,
        rightMargin=1.5 * cm,
        leftMargin=1.5 * cm,
        topMargin=1.9 * cm,
        bottomMargin=1.7 * cm,
        title="System Architecture and Technical Documentation – User Management Module",
        author="GitHub Copilot",
    )

    story = parse_markdown_to_story(md_text)
    doc.build(story, onFirstPage=draw_cover, onLaterPages=draw_page_chrome)

    print(f"PDF generated: {pdf_path}")


if __name__ == "__main__":
    main()

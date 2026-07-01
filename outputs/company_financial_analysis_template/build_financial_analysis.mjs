import fs from "node:fs/promises";
import path from "node:path";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const outputDir = path.resolve("D:/Ban_sao_du_an/outputs/company_financial_analysis_template");
const workbook = Workbook.create();

const sheets = {
  cover: workbook.worksheets.add("Cover"),
  dashboard: workbook.worksheets.add("Dashboard"),
  assumptions: workbook.worksheets.add("Assumptions"),
  financials: workbook.worksheets.add("Historical_Financials"),
  ratios: workbook.worksheets.add("Ratios"),
  valuation: workbook.worksheets.add("Valuation_DCF"),
  sensitivity: workbook.worksheets.add("Sensitivity"),
  checks: workbook.worksheets.add("Checks"),
  sources: workbook.worksheets.add("Sources"),
};

const colors = {
  navy: "#17324D",
  slate: "#334155",
  blue: "#0000FF",
  green: "#008000",
  red: "#FF0000",
  white: "#FFFFFF",
  black: "#000000",
  lightBlue: "#EAF3FF",
  lightTeal: "#E7F5F1",
  paleYellow: "#FFFF00",
  paleGray: "#F3F6F8",
  midGray: "#D9E2EC",
  softGreen: "#E6F4EA",
  softRed: "#FCE8E6",
};

const formats = {
  currency: '"$"#,##0;[Red]("$"#,##0);-',
  percent: "0.0%;[Red](0.0%);-",
  multiple: "0.0x;[Red](0.0x);-",
  number: "#,##0;[Red](#,##0);-",
  decimal: "#,##0.0;[Red](#,##0.0);-",
  perShare: '"$"0.00;[Red]("$"0.00);-',
  date: "yyyy-mm-dd",
};

function title(sheet, range, text) {
  const r = sheet.getRange(range);
  r.merge();
  r.values = [[text]];
  r.format = {
    fill: colors.navy,
    font: { bold: true, color: colors.white, size: 16 },
    horizontalAlignment: "left",
    verticalAlignment: "middle",
  };
  r.format.rowHeight = 30;
}

function section(sheet, range, text) {
  const r = sheet.getRange(range);
  r.merge();
  r.values = [[text]];
  r.format = {
    fill: colors.slate,
    font: { bold: true, color: colors.white },
    horizontalAlignment: "left",
  };
}

function header(range) {
  range.format = {
    fill: colors.lightBlue,
    font: { bold: true, color: colors.black },
    horizontalAlignment: "center",
    borders: { preset: "inside", style: "thin", color: colors.midGray },
  };
}

function input(range) {
  range.format = {
    fill: colors.paleYellow,
    font: { color: colors.blue },
    borders: { preset: "outside", style: "thin", color: colors.midGray },
  };
}

function formula(range) {
  range.format = {
    font: { color: colors.black },
  };
}

function linkFormula(range) {
  range.format = {
    font: { color: colors.green },
  };
}

function total(range) {
  range.format = {
    fill: colors.paleGray,
    font: { bold: true, color: colors.black },
    borders: { preset: "doubleBottom", style: "thin", color: colors.slate },
  };
}

function note(range) {
  range.format = {
    fill: "#FAFAFA",
    font: { color: "#475569", italic: true, size: 9 },
    wrapText: true,
    verticalAlignment: "top",
  };
}

function setWidths(sheet, widths) {
  for (const [col, width] of Object.entries(widths)) {
    sheet.getRange(`${col}:${col}`).format.columnWidth = width;
  }
}

function hideGridlines() {
  Object.values(sheets).forEach((sheet) => {
    sheet.showGridLines = false;
  });
}

function addComment(cellRange, text) {
  try {
    workbook.comments.addThread({ cell: cellRange }, text);
  } catch {
    // Comments are helpful but not model-critical. Source URLs also live on Sources.
  }
}

async function buildCover() {
  const s = sheets.cover;
  title(s, "A1:H1", "Company Financial Analysis Workbook");
  setWidths(s, { A: 24, B: 20, C: 18, D: 18, E: 18, F: 18, G: 18, H: 18 });

  s.getRange("A3:B10").values = [
    ["Purpose", "Analyze a company's historical performance, ratios, forecast, DCF valuation, and scenario sensitivity."],
    ["Company", "='Assumptions'!$B$5"],
    ["Currency / Units", "='Assumptions'!$B$6&\" / \"&'Assumptions'!$B$7"],
    ["Scenario", "='Assumptions'!$B$25"],
    ["Analysis Date", new Date("2026-07-01T00:00:00")],
    ["Model Status", "='Checks'!$B$3"],
    ["Version", "v1.0 template"],
    ["Prepared For", "User"],
  ];
  s.getRange("A3:A10").format = { font: { bold: true }, fill: colors.paleGray };
  s.getRange("B3:B10").format.wrapText = true;
  s.getRange("B4:B8").format.font = { color: colors.green };
  s.getRange("B7").setNumberFormat(formats.date);
  s.getRange("B8").format = { font: { bold: true }, fill: colors.softGreen };

  section(s, "D3:H3", "Key Outputs");
  s.getRange("D4:H11").values = [
    ["Metric", "Value", "Unit", "Source", "Notes"],
    ["DCF Implied Share Price", null, "per share", "Valuation_DCF", "Formula-driven from forecast FCF and terminal value."],
    ["Current Share Price", null, "per share", "Assumptions", "Editable input."],
    ["Upside / Downside", null, "%", "Valuation_DCF", "Compares implied value to current share price."],
    ["Enterprise Value", null, "$mm", "Valuation_DCF", "DCF enterprise value."],
    ["EV / EBITDA", null, "x", "Valuation_DCF", "Enterprise value divided by final forecast EBITDA."],
    ["LTM EBITDA Margin", null, "%", "Historical_Financials", "Latest actual EBITDA margin."],
    ["Net Debt / EBITDA", null, "x", "Ratios", "Latest actual net debt divided by latest actual EBITDA."],
  ];
  header(s.getRange("D4:H4"));
  s.getRange("E5:E11").formulas = [
    ["='Valuation_DCF'!$B$31"],
    ["='Valuation_DCF'!$B$32"],
    ["='Valuation_DCF'!$B$33"],
    ["='Valuation_DCF'!$B$27"],
    ["='Valuation_DCF'!$B$34"],
    ["='Historical_Financials'!$G$13"],
    ["='Ratios'!$G$11"],
  ];
  s.getRange("E5:E6").setNumberFormat(formats.perShare);
  s.getRange("E7").setNumberFormat(formats.percent);
  s.getRange("E8").setNumberFormat(formats.currency);
  s.getRange("E9").setNumberFormat(formats.multiple);
  s.getRange("E10").setNumberFormat(formats.percent);
  s.getRange("E11").setNumberFormat(formats.multiple);
  linkFormula(s.getRange("E5:E11"));

  section(s, "A13:H13", "Color Convention");
  s.getRange("A14:H18").values = [
    ["Style", "Meaning", null, null, null, null, null, null],
    ["Blue text + yellow fill", "Editable inputs and assumptions to replace with company-specific data.", null, null, null, null, null, null],
    ["Black text", "Formulas and calculations.", null, null, null, null, null, null],
    ["Green text", "Links to other worksheets in this workbook.", null, null, null, null, null, null],
    ["Sources tab", "Use plain URLs and source notes for filings, investor materials, and market data.", null, null, null, null, null, null],
  ];
  s.getRange("A14:H14").format = { fill: colors.lightBlue, font: { bold: true } };
  s.getRange("A15").format = { fill: colors.paleYellow, font: { color: colors.blue } };
  s.getRange("A16").format = { font: { color: colors.black } };
  s.getRange("A17").format = { font: { color: colors.green } };
  note(s.getRange("A20:H22"));
  s.getRange("A20:H22").merge();
  s.getRange("A20").values = [[
    "Template note: no company name, ticker, or source data was provided in the request. The historical figures are illustrative placeholders. Replace blue/yellow input cells with company-reported data and add source URLs on the Sources sheet.",
  ]];
}

async function buildAssumptions() {
  const s = sheets.assumptions;
  title(s, "A1:H1", "Assumptions");
  setWidths(s, { A: 36, B: 18, C: 28, D: 3, E: 22, F: 16, G: 16, H: 16 });

  section(s, "A3:C3", "Model Setup");
  s.getRange("A4:C25").values = [
    ["Input", "Value", "Units / Notes"],
    ["Company Name", "[Company Name]", "Replace with target company"],
    ["Currency", "USD", "ISO currency"],
    ["Units", "$mm", "Amounts in millions"],
    ["Fiscal Year End", "Dec", "Month"],
    ["Valuation Date", new Date("2026-07-01T00:00:00"), "yyyy-mm-dd"],
    ["Source Data As Of", new Date("2026-07-01T00:00:00"), "Update after importing filings"],
    ["Current Share Price", 100, "$ / share"],
    ["Diluted Shares", 100, "mm shares"],
    ["Cash & Equivalents", 250, "$mm"],
    ["Total Debt", 500, "$mm"],
    ["Tax Rate", null, "%"],
    ["WACC", null, "%"],
    ["Terminal Growth", null, "%"],
    ["Exit EBITDA Multiple", null, "x"],
    ["Forecast Revenue CAGR", null, "%"],
    ["Target EBITDA Margin", null, "%"],
    ["D&A as % Revenue", 0.04, "% of revenue"],
    ["Capex as % Revenue", 0.05, "% of revenue"],
    ["NWC Investment as % Revenue Change", 0.08, "% of revenue delta"],
    ["Terminal Method", "Gordon Growth", "Gordon Growth / Exit Multiple"],
    ["Selected Scenario", "Base", "Downside / Base / Upside"],
  ];
  header(s.getRange("A4:C4"));
  input(s.getRange("B5:B14"));
  input(s.getRange("B21:B25"));
  s.getRange("B9:B10").setNumberFormat(formats.date);
  s.getRange("B11").setNumberFormat(formats.perShare);
  s.getRange("B12").setNumberFormat(formats.decimal);
  s.getRange("B13:B14").setNumberFormat(formats.currency);
  s.getRange("B15:B17").setNumberFormat(formats.percent);
  s.getRange("B18").setNumberFormat(formats.multiple);
  s.getRange("B19:B23").setNumberFormat(formats.percent);
  s.getRange("B25").dataValidation = { rule: { type: "list", values: ["Downside", "Base", "Upside"] } };
  s.getRange("B24").dataValidation = { rule: { type: "list", values: ["Gordon Growth", "Exit Multiple"] } };

  section(s, "E3:H3", "Scenario Drivers");
  s.getRange("E4:H11").values = [
    ["Driver", "Downside", "Base", "Upside"],
    ["Tax Rate", 0.26, 0.25, 0.24],
    ["WACC", 0.11, 0.09, 0.08],
    ["Terminal Growth", 0.02, 0.03, 0.04],
    ["Exit EBITDA Multiple", 9, 12, 15],
    ["Forecast Revenue CAGR", 0.03, 0.08, 0.12],
    ["Target EBITDA Margin", 0.17, 0.22, 0.27],
    ["Source / Notes", "Stress case", "Management case", "Upside case"],
  ];
  header(s.getRange("E4:H4"));
  input(s.getRange("F5:H10"));
  s.getRange("F5:H7").setNumberFormat(formats.percent);
  s.getRange("F8:H8").setNumberFormat(formats.multiple);
  s.getRange("F9:H10").setNumberFormat(formats.percent);
  s.getRange("B15:B20").formulas = [
    ["=INDEX($F$5:$H$5,1,MATCH($B$25,$F$4:$H$4,0))"],
    ["=INDEX($F$6:$H$6,1,MATCH($B$25,$F$4:$H$4,0))"],
    ["=INDEX($F$7:$H$7,1,MATCH($B$25,$F$4:$H$4,0))"],
    ["=INDEX($F$8:$H$8,1,MATCH($B$25,$F$4:$H$4,0))"],
    ["=INDEX($F$9:$H$9,1,MATCH($B$25,$F$4:$H$4,0))"],
    ["=INDEX($F$10:$H$10,1,MATCH($B$25,$F$4:$H$4,0))"],
  ];
  formula(s.getRange("B15:B20"));

  note(s.getRange("E13:H17"));
  s.getRange("E13:H17").merge();
  s.getRange("E13").values = [[
    "Scenario table values are editable. Selected assumptions in B15:B20 use INDEX/MATCH so the dashboard, forecast, valuation, and sensitivity analysis update when the scenario selector changes.",
  ]];

  addComment(s.getRange("B11"), "Source: replace with current market price source URL on Sources sheet. The current value is illustrative.");
  addComment(s.getRange("B12"), "Source: replace with diluted share count from the company's latest filing.");
  addComment(s.getRange("B13"), "Source: replace with cash and equivalents from latest balance sheet.");
  addComment(s.getRange("B14"), "Source: replace with total debt from latest balance sheet.");
  addComment(s.getRange("F5:H10"), "Assumption: illustrative scenario drivers. Replace with analyst case assumptions, management guidance, or company-specific model drivers.");
}

async function buildFinancials() {
  const s = sheets.financials;
  title(s, "A1:L1", "Historical Financials and Forecast");
  setWidths(s, { A: 34, B: 14, C: 14, D: 14, E: 14, F: 14, G: 14, H: 14, I: 14, J: 14, K: 14, L: 14 });
  s.freezePanes.freezeRows(6);
  s.freezePanes.freezeColumns(1);

  section(s, "A3:L3", "Time Series");
  s.getRange("A4:L6").values = [
    ["Metric", "2020A", "2021A", "2022A", "2023A", "2024A", "2025A", "2026E", "2027E", "2028E", "2029E", "2030E"],
    ["Period Type", "Actual", "Actual", "Actual", "Actual", "Actual", "Actual", "Forecast", "Forecast", "Forecast", "Forecast", "Forecast"],
    ["Source", "Placeholder", "Placeholder", "Placeholder", "Placeholder", "Placeholder", "Placeholder", "Formula", "Formula", "Formula", "Formula", "Formula"],
  ];
  header(s.getRange("A4:L6"));

  s.getRange("A8:L29").values = [
    ["Revenue", 1000, 1120, 1280, 1450, 1660, 1900, null, null, null, null, null],
    ["Revenue Growth", null, null, null, null, null, null, null, null, null, null, null],
    ["Gross Profit", 560, 638, 742, 856, 996, 1169, null, null, null, null, null],
    ["Gross Margin", null, null, null, null, null, null, null, null, null, null, null],
    ["EBITDA", 150, 180, 224, 268, 332, 399, null, null, null, null, null],
    ["EBITDA Margin", null, null, null, null, null, null, null, null, null, null, null],
    ["EBIT", 110, 135, 174, 210, 265, 323, null, null, null, null, null],
    ["EBIT Margin", null, null, null, null, null, null, null, null, null, null, null],
    ["Net Income", 78, 96, 125, 151, 191, 233, null, null, null, null, null],
    ["Net Margin", null, null, null, null, null, null, null, null, null, null, null],
    ["", null, null, null, null, null, null, null, null, null, null, null],
    ["Cash & Equivalents", 140, 155, 175, 205, 230, 250, null, null, null, null, null],
    ["Total Debt", 420, 435, 450, 470, 490, 500, null, null, null, null, null],
    ["Net Debt", null, null, null, null, null, null, null, null, null, null, null],
    ["Diluted Shares", 100, 100, 100, 100, 100, 100, null, null, null, null, null],
    ["EPS", null, null, null, null, null, null, null, null, null, null, null],
    ["", null, null, null, null, null, null, null, null, null, null, null],
    ["Operating Cash Flow", 120, 145, 175, 212, 255, 308, null, null, null, null, null],
    ["Capital Expenditure", 55, 60, 68, 77, 88, 100, null, null, null, null, null],
    ["Free Cash Flow", null, null, null, null, null, null, null, null, null, null, null],
    ["FCF Margin", null, null, null, null, null, null, null, null, null, null, null],
    ["D&A", null, null, null, null, null, null, null, null, null, null, null],
  ];
  s.getRange("B8:G8").format.font = { color: colors.blue };
  s.getRange("B10:G10").format.font = { color: colors.blue };
  s.getRange("B12:G12").format.font = { color: colors.blue };
  s.getRange("B14:G14").format.font = { color: colors.blue };
  s.getRange("B16:G16").format.font = { color: colors.blue };
  s.getRange("B19:G20").format.font = { color: colors.blue };
  s.getRange("B22:G22").format.font = { color: colors.blue };
  s.getRange("B25:G26").format.font = { color: colors.blue };
  // Historical input styling is applied after total-row formatting so formula rows stay visually distinct.

  s.getRange("C9:G9").formulas = [["=C8/B8-1", "=D8/C8-1", "=E8/D8-1", "=F8/E8-1", "=G8/F8-1"]];
  s.getRange("B11:G11").formulas = [["=B10/B8", "=C10/C8", "=D10/D8", "=E10/E8", "=F10/F8", "=G10/G8"]];
  s.getRange("B13:G13").formulas = [["=B12/B8", "=C12/C8", "=D12/D8", "=E12/E8", "=F12/F8", "=G12/G8"]];
  s.getRange("B15:G15").formulas = [["=B14/B8", "=C14/C8", "=D14/D8", "=E14/E8", "=F14/F8", "=G14/G8"]];
  s.getRange("B17:G17").formulas = [["=B16/B8", "=C16/C8", "=D16/D8", "=E16/E8", "=F16/F8", "=G16/G8"]];
  s.getRange("B21:G21").formulas = [["=B20-B19", "=C20-C19", "=D20-D19", "=E20-E19", "=F20-F19", "=G20-G19"]];
  s.getRange("B23:G23").formulas = [["=B16/B22", "=C16/C22", "=D16/D22", "=E16/E22", "=F16/F22", "=G16/G22"]];
  s.getRange("B27:G27").formulas = [["=B25-B26", "=C25-C26", "=D25-D26", "=E25-E26", "=F25-F26", "=G25-G26"]];
  s.getRange("B28:G28").formulas = [["=B27/B8", "=C27/C8", "=D27/D8", "=E27/E8", "=F27/F8", "=G27/G8"]];
  s.getRange("B29:G29").formulas = [["=B12-B14", "=C12-C14", "=D12-D14", "=E12-E14", "=F12-F14", "=G12-G14"]];

  s.getRange("H8").formulas = [["=G8*(1+'Assumptions'!$B$19)"]];
  s.getRange("H8:L8").fillRight();
  s.getRange("H9").formulas = [["=H8/G8-1"]];
  s.getRange("H9:L9").fillRight();
  s.getRange("H10").formulas = [["=H8*$G$11"]];
  s.getRange("H10:L10").fillRight();
  s.getRange("H11").formulas = [["=H10/H8"]];
  s.getRange("H11:L11").fillRight();
  s.getRange("H12").formulas = [["=H8*($G$13+('Assumptions'!$B$20-$G$13)*(COLUMNS($H:H)/5))"]];
  s.getRange("H12:L12").fillRight();
  s.getRange("H13").formulas = [["=H12/H8"]];
  s.getRange("H13:L13").fillRight();
  s.getRange("H29").formulas = [["=H8*'Assumptions'!$B$21"]];
  s.getRange("H29:L29").fillRight();
  s.getRange("H14").formulas = [["=H12-H29"]];
  s.getRange("H14:L14").fillRight();
  s.getRange("H15").formulas = [["=H14/H8"]];
  s.getRange("H15:L15").fillRight();
  s.getRange("H16").formulas = [["=H14*(1-'Assumptions'!$B$15)"]];
  s.getRange("H16:L16").fillRight();
  s.getRange("H17").formulas = [["=H16/H8"]];
  s.getRange("H17:L17").fillRight();
  s.getRange("H19").formulas = [["=G19+H27"]];
  s.getRange("H19:L19").fillRight();
  s.getRange("H20").formulas = [["=G20"]];
  s.getRange("H20:L20").fillRight();
  s.getRange("H21").formulas = [["=H20-H19"]];
  s.getRange("H21:L21").fillRight();
  s.getRange("H22").formulas = [["='Assumptions'!$B$12"]];
  s.getRange("H22:L22").fillRight();
  s.getRange("H23").formulas = [["=H16/H22"]];
  s.getRange("H23:L23").fillRight();
  s.getRange("H25").formulas = [["=H16+H29-(H8-G8)*'Assumptions'!$B$23"]];
  s.getRange("H25:L25").fillRight();
  s.getRange("H26").formulas = [["=H8*'Assumptions'!$B$22"]];
  s.getRange("H26:L26").fillRight();
  s.getRange("H27").formulas = [["=H25-H26"]];
  s.getRange("H27:L27").fillRight();
  s.getRange("H28").formulas = [["=H27/H8"]];
  s.getRange("H28:L28").fillRight();

  formula(s.getRange("B9:L29"));
  linkFormula(s.getRange("H8:L29"));
  s.getRange("A8:A29").format = { font: { bold: false } };
  total(s.getRange("A8:L8"));
  total(s.getRange("A12:L12"));
  total(s.getRange("A16:L16"));
  total(s.getRange("A21:L21"));
  total(s.getRange("A27:L27"));
  s.getRange("B8:L8").setNumberFormat(formats.currency);
  s.getRange("B10:L10").setNumberFormat(formats.currency);
  s.getRange("B12:L12").setNumberFormat(formats.currency);
  s.getRange("B14:L14").setNumberFormat(formats.currency);
  s.getRange("B16:L16").setNumberFormat(formats.currency);
  s.getRange("B19:L21").setNumberFormat(formats.currency);
  s.getRange("B22:L22").setNumberFormat(formats.decimal);
  s.getRange("B23:L23").setNumberFormat(formats.perShare);
  s.getRange("B25:L27").setNumberFormat(formats.currency);
  s.getRange("B29:L29").setNumberFormat(formats.currency);
  s.getRange("B9:L9").setNumberFormat(formats.percent);
  s.getRange("B11:L11").setNumberFormat(formats.percent);
  s.getRange("B13:L13").setNumberFormat(formats.percent);
  s.getRange("B15:L15").setNumberFormat(formats.percent);
  s.getRange("B17:L17").setNumberFormat(formats.percent);
  s.getRange("B28:L28").setNumberFormat(formats.percent);
  s.getRange("H4:L4").format.fill = colors.lightTeal;

  [
    "B8:G8",
    "B10:G10",
    "B12:G12",
    "B14:G14",
    "B16:G16",
    "B19:G20",
    "B22:G22",
    "B25:G26",
  ].forEach((range) => input(s.getRange(range)));

  note(s.getRange("A31:L34"));
  s.getRange("A31:L34").merge();
  s.getRange("A31").values = [[
    "Replace yellow historical input cells with actual company financials. Forecast formulas use scenario assumptions from Assumptions. Amounts are in the selected units, currently $mm.",
  ]];

  addComment(s.getRange("B8:G26"), "Source: illustrative placeholder historicals. Replace with reported financial statements and cite filing/investor presentation URLs on Sources.");
}

async function buildRatios() {
  const s = sheets.ratios;
  title(s, "A1:L1", "Ratio Analysis");
  setWidths(s, { A: 32, B: 14, C: 14, D: 14, E: 14, F: 14, G: 14, H: 14, I: 14, J: 14, K: 14, L: 14 });
  s.freezePanes.freezeRows(5);
  s.freezePanes.freezeColumns(1);

  section(s, "A3:L3", "Profitability, Cash Flow, and Leverage");
  s.getRange("A4").values = [["Metric"]];
  s.getRange("B4:L4").formulas = [[
    "='Historical_Financials'!B4",
    "='Historical_Financials'!C4",
    "='Historical_Financials'!D4",
    "='Historical_Financials'!E4",
    "='Historical_Financials'!F4",
    "='Historical_Financials'!G4",
    "='Historical_Financials'!H4",
    "='Historical_Financials'!I4",
    "='Historical_Financials'!J4",
    "='Historical_Financials'!K4",
    "='Historical_Financials'!L4",
  ]];
  header(s.getRange("A4:L4"));
  linkFormula(s.getRange("B4:L4"));
  s.getRange("A6:L14").values = [
    ["Revenue Growth", null, null, null, null, null, null, null, null, null, null, null],
    ["Gross Margin", null, null, null, null, null, null, null, null, null, null, null],
    ["EBITDA Margin", null, null, null, null, null, null, null, null, null, null, null],
    ["Net Margin", null, null, null, null, null, null, null, null, null, null, null],
    ["FCF Margin", null, null, null, null, null, null, null, null, null, null, null],
    ["Net Debt / EBITDA", null, null, null, null, null, null, null, null, null, null, null],
    ["FCF Conversion", null, null, null, null, null, null, null, null, null, null, null],
    ["D&A / Revenue", null, null, null, null, null, null, null, null, null, null, null],
    ["Capex / Revenue", null, null, null, null, null, null, null, null, null, null, null],
  ];
  s.getRange("B6:L14").formulas = [
    ["='Historical_Financials'!B9", "='Historical_Financials'!C9", "='Historical_Financials'!D9", "='Historical_Financials'!E9", "='Historical_Financials'!F9", "='Historical_Financials'!G9", "='Historical_Financials'!H9", "='Historical_Financials'!I9", "='Historical_Financials'!J9", "='Historical_Financials'!K9", "='Historical_Financials'!L9"],
    ["='Historical_Financials'!B11", "='Historical_Financials'!C11", "='Historical_Financials'!D11", "='Historical_Financials'!E11", "='Historical_Financials'!F11", "='Historical_Financials'!G11", "='Historical_Financials'!H11", "='Historical_Financials'!I11", "='Historical_Financials'!J11", "='Historical_Financials'!K11", "='Historical_Financials'!L11"],
    ["='Historical_Financials'!B13", "='Historical_Financials'!C13", "='Historical_Financials'!D13", "='Historical_Financials'!E13", "='Historical_Financials'!F13", "='Historical_Financials'!G13", "='Historical_Financials'!H13", "='Historical_Financials'!I13", "='Historical_Financials'!J13", "='Historical_Financials'!K13", "='Historical_Financials'!L13"],
    ["='Historical_Financials'!B17", "='Historical_Financials'!C17", "='Historical_Financials'!D17", "='Historical_Financials'!E17", "='Historical_Financials'!F17", "='Historical_Financials'!G17", "='Historical_Financials'!H17", "='Historical_Financials'!I17", "='Historical_Financials'!J17", "='Historical_Financials'!K17", "='Historical_Financials'!L17"],
    ["='Historical_Financials'!B28", "='Historical_Financials'!C28", "='Historical_Financials'!D28", "='Historical_Financials'!E28", "='Historical_Financials'!F28", "='Historical_Financials'!G28", "='Historical_Financials'!H28", "='Historical_Financials'!I28", "='Historical_Financials'!J28", "='Historical_Financials'!K28", "='Historical_Financials'!L28"],
    ["='Historical_Financials'!B21/'Historical_Financials'!B12", "='Historical_Financials'!C21/'Historical_Financials'!C12", "='Historical_Financials'!D21/'Historical_Financials'!D12", "='Historical_Financials'!E21/'Historical_Financials'!E12", "='Historical_Financials'!F21/'Historical_Financials'!F12", "='Historical_Financials'!G21/'Historical_Financials'!G12", "='Historical_Financials'!H21/'Historical_Financials'!H12", "='Historical_Financials'!I21/'Historical_Financials'!I12", "='Historical_Financials'!J21/'Historical_Financials'!J12", "='Historical_Financials'!K21/'Historical_Financials'!K12", "='Historical_Financials'!L21/'Historical_Financials'!L12"],
    ["='Historical_Financials'!B27/'Historical_Financials'!B12", "='Historical_Financials'!C27/'Historical_Financials'!C12", "='Historical_Financials'!D27/'Historical_Financials'!D12", "='Historical_Financials'!E27/'Historical_Financials'!E12", "='Historical_Financials'!F27/'Historical_Financials'!F12", "='Historical_Financials'!G27/'Historical_Financials'!G12", "='Historical_Financials'!H27/'Historical_Financials'!H12", "='Historical_Financials'!I27/'Historical_Financials'!I12", "='Historical_Financials'!J27/'Historical_Financials'!J12", "='Historical_Financials'!K27/'Historical_Financials'!K12", "='Historical_Financials'!L27/'Historical_Financials'!L12"],
    ["='Historical_Financials'!B29/'Historical_Financials'!B8", "='Historical_Financials'!C29/'Historical_Financials'!C8", "='Historical_Financials'!D29/'Historical_Financials'!D8", "='Historical_Financials'!E29/'Historical_Financials'!E8", "='Historical_Financials'!F29/'Historical_Financials'!F8", "='Historical_Financials'!G29/'Historical_Financials'!G8", "='Historical_Financials'!H29/'Historical_Financials'!H8", "='Historical_Financials'!I29/'Historical_Financials'!I8", "='Historical_Financials'!J29/'Historical_Financials'!J8", "='Historical_Financials'!K29/'Historical_Financials'!K8", "='Historical_Financials'!L29/'Historical_Financials'!L8"],
    ["='Historical_Financials'!B26/'Historical_Financials'!B8", "='Historical_Financials'!C26/'Historical_Financials'!C8", "='Historical_Financials'!D26/'Historical_Financials'!D8", "='Historical_Financials'!E26/'Historical_Financials'!E8", "='Historical_Financials'!F26/'Historical_Financials'!F8", "='Historical_Financials'!G26/'Historical_Financials'!G8", "='Historical_Financials'!H26/'Historical_Financials'!H8", "='Historical_Financials'!I26/'Historical_Financials'!I8", "='Historical_Financials'!J26/'Historical_Financials'!J8", "='Historical_Financials'!K26/'Historical_Financials'!K8", "='Historical_Financials'!L26/'Historical_Financials'!L8"],
  ];
  linkFormula(s.getRange("B6:L14"));
  s.getRange("B6:L10").setNumberFormat(formats.percent);
  s.getRange("B11:L11").setNumberFormat(formats.multiple);
  s.getRange("B12:L14").setNumberFormat(formats.percent);

  note(s.getRange("A16:L18"));
  s.getRange("A16:L18").merge();
  s.getRange("A16").values = [[
    "Ratios link to Historical_Financials so updates to reported values or forecast assumptions flow through automatically. Add industry-specific KPIs as new rows if the company has important operating metrics.",
  ]];
}

async function buildValuation() {
  const s = sheets.valuation;
  title(s, "A1:H1", "DCF Valuation");
  setWidths(s, { A: 34, B: 15, C: 15, D: 15, E: 15, F: 15, G: 18, H: 24 });
  s.freezePanes.freezeRows(5);
  s.freezePanes.freezeColumns(1);

  section(s, "A3:F3", "Forecast Free Cash Flow");
  s.getRange("A4").values = [["Metric"]];
  s.getRange("B4:F4").formulas = [[
    "='Historical_Financials'!H4",
    "='Historical_Financials'!I4",
    "='Historical_Financials'!J4",
    "='Historical_Financials'!K4",
    "='Historical_Financials'!L4",
  ]];
  header(s.getRange("A4:F4"));
  s.getRange("A6:F16").values = [
    ["Revenue", null, null, null, null, null],
    ["EBITDA", null, null, null, null, null],
    ["EBIT", null, null, null, null, null],
    ["Tax on EBIT", null, null, null, null, null],
    ["NOPAT", null, null, null, null, null],
    ["D&A", null, null, null, null, null],
    ["Capital Expenditure", null, null, null, null, null],
    ["Change in NWC", null, null, null, null, null],
    ["Unlevered FCF", null, null, null, null, null],
    ["Discount Factor", null, null, null, null, null],
    ["PV of FCF", null, null, null, null, null],
  ];
  s.getRange("B6:F16").formulas = [
    ["='Historical_Financials'!H8", "='Historical_Financials'!I8", "='Historical_Financials'!J8", "='Historical_Financials'!K8", "='Historical_Financials'!L8"],
    ["='Historical_Financials'!H12", "='Historical_Financials'!I12", "='Historical_Financials'!J12", "='Historical_Financials'!K12", "='Historical_Financials'!L12"],
    ["='Historical_Financials'!H14", "='Historical_Financials'!I14", "='Historical_Financials'!J14", "='Historical_Financials'!K14", "='Historical_Financials'!L14"],
    ["=B8*'Assumptions'!$B$15", "=C8*'Assumptions'!$B$15", "=D8*'Assumptions'!$B$15", "=E8*'Assumptions'!$B$15", "=F8*'Assumptions'!$B$15"],
    ["=B8-B9", "=C8-C9", "=D8-D9", "=E8-E9", "=F8-F9"],
    ["='Historical_Financials'!H29", "='Historical_Financials'!I29", "='Historical_Financials'!J29", "='Historical_Financials'!K29", "='Historical_Financials'!L29"],
    ["='Historical_Financials'!H26", "='Historical_Financials'!I26", "='Historical_Financials'!J26", "='Historical_Financials'!K26", "='Historical_Financials'!L26"],
    ["=(B6-'Historical_Financials'!G8)*'Assumptions'!$B$23", "=(C6-B6)*'Assumptions'!$B$23", "=(D6-C6)*'Assumptions'!$B$23", "=(E6-D6)*'Assumptions'!$B$23", "=(F6-E6)*'Assumptions'!$B$23"],
    ["=B10+B11-B12-B13", "=C10+C11-C12-C13", "=D10+D11-D12-D13", "=E10+E11-E12-E13", "=F10+F11-F12-F13"],
    ["=1/(1+'Assumptions'!$B$16)^1", "=1/(1+'Assumptions'!$B$16)^2", "=1/(1+'Assumptions'!$B$16)^3", "=1/(1+'Assumptions'!$B$16)^4", "=1/(1+'Assumptions'!$B$16)^5"],
    ["=B14*B15", "=C14*C15", "=D14*D15", "=E14*E15", "=F14*F15"],
  ];
  formula(s.getRange("B6:F16"));
  s.getRange("B6:F14").setNumberFormat(formats.currency);
  s.getRange("B15:F15").setNumberFormat("0.000x");
  s.getRange("B16:F16").setNumberFormat(formats.currency);
  total(s.getRange("A14:F14"));
  total(s.getRange("A16:F16"));

  section(s, "A19:H19", "Enterprise Value Bridge");
  s.getRange("A20:H35").values = [
    ["Metric", "Value", "Format", "Source / Formula", null, null, null, null],
    ["Terminal EBITDA", null, "$mm", "Final forecast EBITDA", null, null, null, null],
    ["Exit Multiple Terminal Value", null, "$mm", "Terminal EBITDA x exit multiple", null, null, null, null],
    ["Gordon Growth Terminal Value", null, "$mm", "Final FCF x (1+g) / (WACC-g)", null, null, null, null],
    ["Selected Terminal Value", null, "$mm", "Based on Terminal Method assumption", null, null, null, null],
    ["PV of Terminal Value", null, "$mm", "Terminal value discounted to valuation date", null, null, null, null],
    ["PV of Forecast FCF", null, "$mm", "Sum of forecast PVs", null, null, null, null],
    ["Enterprise Value", null, "$mm", "PV FCF + PV Terminal Value", null, null, null, null],
    ["Less: Net Debt", null, "$mm", "Debt minus cash", null, null, null, null],
    ["Equity Value", null, "$mm", "Enterprise value minus net debt", null, null, null, null],
    ["Diluted Shares", null, "mm", "Assumptions", null, null, null, null],
    ["Implied Share Price", null, "$ / share", "Equity value / shares", null, null, null, null],
    ["Current Share Price", null, "$ / share", "Assumptions", null, null, null, null],
    ["Upside / Downside", null, "%", "Implied / current - 1", null, null, null, null],
    ["Implied EV / EBITDA", null, "x", "Enterprise value / final EBITDA", null, null, null, null],
    ["Implied EV / Revenue", null, "x", "Enterprise value / final revenue", null, null, null, null],
  ];
  header(s.getRange("A20:D20"));
  s.getRange("B21:B35").formulas = [
    ["=F7"],
    ["=B21*'Assumptions'!$B$18"],
    ["=IF('Assumptions'!$B$16<='Assumptions'!$B$17,NA(),F14*(1+'Assumptions'!$B$17)/('Assumptions'!$B$16-'Assumptions'!$B$17))"],
    ["=IF('Assumptions'!$B$24=\"Exit Multiple\",B22,B23)"],
    ["=B24*F15"],
    ["=SUM(B16:F16)"],
    ["=B25+B26"],
    ["='Assumptions'!$B$14-'Assumptions'!$B$13"],
    ["=B27-B28"],
    ["='Assumptions'!$B$12"],
    ["=B29/B30"],
    ["='Assumptions'!$B$11"],
    ["=B31/B32-1"],
    ["=B27/B21"],
    ["=B27/F6"],
  ];
  formula(s.getRange("B21:B35"));
  s.getRange("B21:B29").setNumberFormat(formats.currency);
  s.getRange("B30").setNumberFormat(formats.decimal);
  s.getRange("B31:B32").setNumberFormat(formats.perShare);
  s.getRange("B33").setNumberFormat(formats.percent);
  s.getRange("B34:B35").setNumberFormat(formats.multiple);
  total(s.getRange("A27:D27"));
  total(s.getRange("A29:D29"));
  total(s.getRange("A31:D31"));

  note(s.getRange("F21:H29"));
  s.getRange("F21:H29").merge();
  s.getRange("F21").values = [[
    "DCF caveat: this template uses simplified operating drivers because no company-specific disclosures were provided. Replace placeholders with actual data, expand working capital/debt schedules as needed, and cite sources before using for investment decisions.",
  ]];
}

async function buildSensitivity() {
  const s = sheets.sensitivity;
  title(s, "A1:H1", "Sensitivity Analysis");
  setWidths(s, { A: 18, B: 14, C: 14, D: 14, E: 14, F: 14, G: 14, H: 18 });

  section(s, "A3:F3", "Implied Share Price: WACC vs Terminal Growth");
  s.getRange("A4:F10").values = [
    ["WACC \\ TGR", 0.01, 0.02, 0.03, 0.04, 0.05],
    [0.07, null, null, null, null, null],
    [0.08, null, null, null, null, null],
    [0.09, null, null, null, null, null],
    [0.10, null, null, null, null, null],
    [0.11, null, null, null, null, null],
    [0.12, null, null, null, null, null],
  ];
  header(s.getRange("A4:F4"));
  header(s.getRange("A5:A10"));
  s.getRange("B5:F10").formulas = Array.from({ length: 6 }, (_, r) =>
    Array.from({ length: 5 }, (_, c) => {
      const row = 5 + r;
      const col = String.fromCharCode("B".charCodeAt(0) + c);
      return `=IF($A${row}<=${col}$4,NA(),(('Valuation_DCF'!$B$14/(1+$A${row})^1+'Valuation_DCF'!$C$14/(1+$A${row})^2+'Valuation_DCF'!$D$14/(1+$A${row})^3+'Valuation_DCF'!$E$14/(1+$A${row})^4+'Valuation_DCF'!$F$14/(1+$A${row})^5)+(('Valuation_DCF'!$F$14*(1+${col}$4)/($A${row}-${col}$4))/(1+$A${row})^5)-'Valuation_DCF'!$B$28)/'Valuation_DCF'!$B$30)`;
    }),
  );
  s.getRange("A5:A10").setNumberFormat(formats.percent);
  s.getRange("B4:F4").setNumberFormat(formats.percent);
  s.getRange("B5:F10").setNumberFormat(formats.perShare);
  formula(s.getRange("B5:F10"));

  section(s, "A13:F13", "Implied Share Price: WACC vs Exit EBITDA Multiple");
  s.getRange("A14:F20").values = [
    ["WACC \\ Exit Multiple", 8, 10, 12, 14, 16],
    [0.07, null, null, null, null, null],
    [0.08, null, null, null, null, null],
    [0.09, null, null, null, null, null],
    [0.10, null, null, null, null, null],
    [0.11, null, null, null, null, null],
    [0.12, null, null, null, null, null],
  ];
  header(s.getRange("A14:F14"));
  header(s.getRange("A15:A20"));
  s.getRange("B15:F20").formulas = Array.from({ length: 6 }, (_, r) =>
    Array.from({ length: 5 }, (_, c) => {
      const row = 15 + r;
      const col = String.fromCharCode("B".charCodeAt(0) + c);
      return `=(('Valuation_DCF'!$B$14/(1+$A${row})^1+'Valuation_DCF'!$C$14/(1+$A${row})^2+'Valuation_DCF'!$D$14/(1+$A${row})^3+'Valuation_DCF'!$E$14/(1+$A${row})^4+'Valuation_DCF'!$F$14/(1+$A${row})^5)+('Valuation_DCF'!$F$7*${col}$14/(1+$A${row})^5)-'Valuation_DCF'!$B$28)/'Valuation_DCF'!$B$30`;
    }),
  );
  s.getRange("A15:A20").setNumberFormat(formats.percent);
  s.getRange("B14:F14").setNumberFormat(formats.multiple);
  s.getRange("B15:F20").setNumberFormat(formats.perShare);
  formula(s.getRange("B15:F20"));

  note(s.getRange("A23:H26"));
  s.getRange("A23:H26").merge();
  s.getRange("A23").values = [[
    "Sensitivity tables recalculate valuation mechanics from row and column drivers. They are not pasted static outputs.",
  ]];
}

async function buildChecks() {
  const s = sheets.checks;
  title(s, "A1:G1", "Model Checks");
  setWidths(s, { A: 30, B: 16, C: 16, D: 16, E: 16, F: 18, G: 36 });

  s.getRange("A3:B3").values = [["MODEL STATUS", null]];
  s.getRange("B3").formulas = [["=IF(COUNTIF(F6:F13,\"FAIL\")=0,\"PASS\",\"FAIL\")"]];
  s.getRange("A3:B3").format = {
    fill: colors.navy,
    font: { bold: true, color: colors.white },
    horizontalAlignment: "center",
  };

  s.getRange("A5:G13").values = [
    ["Check", "Actual", "Expected", "Difference", "Tolerance", "Status", "Notes"],
    ["Scenario selector valid", null, 1, null, 0, null, "Selected scenario must match Downside/Base/Upside."],
    ["Terminal spread positive", null, 1, null, 0, null, "WACC must exceed terminal growth for Gordon Growth method."],
    ["Revenue forecast positive", null, 1, null, 0, null, "All forecast revenue periods should be > 0."],
    ["Latest actual revenue populated", null, 1, null, 0, null, "Replace placeholders with actual latest year revenue."],
    ["Latest actual EBITDA populated", null, 1, null, 0, null, "Replace placeholders with actual latest year EBITDA."],
    ["DCF output populated", null, 1, null, 0, null, "Implied share price should calculate."],
    ["Net debt bridge", null, null, null, 0.1, null, "Net debt equals debt minus cash."],
    ["FCF tie-out latest forecast", null, null, null, 0.1, null, "FCF equals NOPAT + D&A - Capex - Change in NWC."],
  ];
  header(s.getRange("A5:G5"));
  s.getRange("B6:F13").formulas = [
    ["=IFERROR(MATCH('Assumptions'!$B$25,'Assumptions'!$F$4:$H$4,0)>0,0)", "=TRUE", "=B6=C6", "=TRUE", "=IF(D6=E6,\"OK\",\"FAIL\")"],
    ["=IF('Assumptions'!$B$16>'Assumptions'!$B$17,TRUE,FALSE)", "=TRUE", "=B7=C7", "=TRUE", "=IF(D7=E7,\"OK\",\"FAIL\")"],
    ["=COUNTIF('Historical_Financials'!$H$8:$L$8,\">0\")", "=5", "=B8-C8", "=0", "=IF(ABS(D8)<=E8,\"OK\",\"FAIL\")"],
    ["=IF('Historical_Financials'!$G$8>0,1,0)", "=1", "=B9-C9", "=0", "=IF(ABS(D9)<=E9,\"OK\",\"FAIL\")"],
    ["=IF('Historical_Financials'!$G$12>0,1,0)", "=1", "=B10-C10", "=0", "=IF(ABS(D10)<=E10,\"OK\",\"FAIL\")"],
    ["=IF('Valuation_DCF'!$B$31>0,1,0)", "=1", "=B11-C11", "=0", "=IF(ABS(D11)<=E11,\"OK\",\"FAIL\")"],
    ["='Historical_Financials'!$G$21", "='Historical_Financials'!$G$20-'Historical_Financials'!$G$19", "=B12-C12", "=0.1", "=IF(ABS(D12)<=E12,\"OK\",\"FAIL\")"],
    ["='Valuation_DCF'!$F$14", "='Valuation_DCF'!$F$10+'Valuation_DCF'!$F$11-'Valuation_DCF'!$F$12-'Valuation_DCF'!$F$13", "=B13-C13", "=0.1", "=IF(ABS(D13)<=E13,\"OK\",\"FAIL\")"],
  ];
  formula(s.getRange("B6:F13"));
  s.getRange("B8:E13").setNumberFormat(formats.decimal);
  s.getRange("B6:E7").setNumberFormat("General");
  s.getRange("F6:F13").format = { font: { bold: true } };
  s.getRange("F6:F13").conditionalFormats.add("containsText", {
    text: "OK",
    format: { fill: colors.softGreen, font: { bold: true, color: "#0F5132" } },
  });
  s.getRange("F6:F13").conditionalFormats.add("containsText", {
    text: "FAIL",
    format: { fill: colors.softRed, font: { bold: true, color: "#842029" } },
  });
  s.getRange("B3").conditionalFormats.add("containsText", {
    text: "PASS",
    format: { fill: colors.softGreen, font: { bold: true, color: "#0F5132" } },
  });
  s.getRange("B3").conditionalFormats.add("containsText", {
    text: "FAIL",
    format: { fill: colors.softRed, font: { bold: true, color: "#842029" } },
  });
}

async function buildSources() {
  const s = sheets.sources;
  title(s, "A1:J1", "Sources and Audit Trail");
  setWidths(s, { A: 36, B: 18, C: 12, D: 18, E: 16, F: 28, G: 52, H: 18, I: 38, J: 20 });
  s.freezePanes.freezeRows(4);

  section(s, "A3:J3", "Source Log");
  s.getRange("A4:J11").values = [
    ["Item", "Value", "Units", "Period / As-of", "Source Type", "Source Name", "Ref / URL", "Owner", "Notes", "Accessed / Refreshed"],
    ["Historical financials", "Placeholder", "$mm", "2020A-2025A", "User input", "Replace with filing data", "https://www.sec.gov/edgar/search/", "User", "Illustrative values only. Replace before analysis use.", new Date("2026-07-01T00:00:00")],
    ["Current share price", 100, "$ / share", "Valuation date", "User input", "Market data", "Add source URL", "User", "Illustrative value.", new Date("2026-07-01T00:00:00")],
    ["Diluted shares", 100, "mm", "Latest filing", "User input", "10-K / 10-Q", "Add source URL", "User", "Illustrative value.", new Date("2026-07-01T00:00:00")],
    ["Cash and equivalents", 250, "$mm", "Latest filing", "User input", "Balance sheet", "Add source URL", "User", "Illustrative value.", new Date("2026-07-01T00:00:00")],
    ["Total debt", 500, "$mm", "Latest filing", "User input", "Balance sheet", "Add source URL", "User", "Illustrative value.", new Date("2026-07-01T00:00:00")],
    ["Scenario drivers", "Placeholder", "% / x", "Forecast", "Assumption", "Analyst case", "Add source URL", "User", "Replace with management guidance or analyst assumptions.", new Date("2026-07-01T00:00:00")],
    ["Tax rate / WACC / Terminal Growth", "Placeholder", "%", "Forecast", "Assumption", "Valuation assumptions", "Add source URL", "User", "Document methodology before investment use.", new Date("2026-07-01T00:00:00")],
  ];
  header(s.getRange("A4:J4"));
  s.getRange("B6:B8").setNumberFormat(formats.decimal);
  s.getRange("J5:J11").setNumberFormat(formats.date);
  s.getRange("G5:G11").format.wrapText = true;
  s.getRange("I5:I11").format.wrapText = true;

  note(s.getRange("A13:J16"));
  s.getRange("A13:J16").merge();
  s.getRange("A13").values = [[
    "When real company data is added, cite primary sources here. For public US companies, SEC EDGAR filings are usually the starting point. Use company investor relations pages for presentations and earnings releases where relevant.",
  ]];
}

async function buildDashboard() {
  const s = sheets.dashboard;
  title(s, "A1:O1", "Financial Dashboard");
  setWidths(s, {
    A: 20,
    B: 16,
    C: 16,
    D: 16,
    E: 16,
    F: 16,
    G: 4,
    H: 16,
    I: 16,
    J: 16,
    K: 16,
    L: 16,
    M: 16,
    N: 16,
    O: 16,
  });

  section(s, "A3:F3", "Headline Metrics");
  s.getRange("A4:F9").values = [
    ["Metric", "Value", "Unit", "Comment", "Metric", "Value"],
    ["Company", null, "", "Input", "Scenario", null],
    ["Model Status", null, "", "Checks", "Current Price", null],
    ["Implied Share Price", null, "$ / share", "DCF", "Upside / Downside", null],
    ["Enterprise Value", null, "$mm", "DCF", "EV / EBITDA", null],
    ["Latest EBITDA Margin", null, "%", "Latest actual", "Net Debt / EBITDA", null],
  ];
  header(s.getRange("A4:F4"));
  s.getRange("B5:B9").formulas = [
    ["='Assumptions'!$B$5"],
    ["='Checks'!$B$3"],
    ["='Valuation_DCF'!$B$31"],
    ["='Valuation_DCF'!$B$27"],
    ["='Historical_Financials'!$G$13"],
  ];
  s.getRange("F5:F9").formulas = [
    ["='Assumptions'!$B$25"],
    ["='Valuation_DCF'!$B$32"],
    ["='Valuation_DCF'!$B$33"],
    ["='Valuation_DCF'!$B$34"],
    ["='Ratios'!$G$11"],
  ];
  linkFormula(s.getRange("B5:B9"));
  linkFormula(s.getRange("F5:F9"));
  s.getRange("B7").setNumberFormat(formats.perShare);
  s.getRange("F6").setNumberFormat(formats.perShare);
  s.getRange("F7").setNumberFormat(formats.percent);
  s.getRange("B8").setNumberFormat(formats.currency);
  s.getRange("F8:F9").setNumberFormat(formats.multiple);
  s.getRange("B9").setNumberFormat(formats.percent);

  section(s, "A11:F11", "Financial Snapshot");
  s.getRange("A12:F24").values = [["Year", "Revenue", "EBITDA", "EBITDA Margin", "Free Cash Flow", "FCF Margin"]];
  header(s.getRange("A12:F12"));
  s.getRange("A13:F23").formulas = [
    ["='Historical_Financials'!B4", "='Historical_Financials'!B8", "='Historical_Financials'!B12", "='Historical_Financials'!B13", "='Historical_Financials'!B27", "='Historical_Financials'!B28"],
    ["='Historical_Financials'!C4", "='Historical_Financials'!C8", "='Historical_Financials'!C12", "='Historical_Financials'!C13", "='Historical_Financials'!C27", "='Historical_Financials'!C28"],
    ["='Historical_Financials'!D4", "='Historical_Financials'!D8", "='Historical_Financials'!D12", "='Historical_Financials'!D13", "='Historical_Financials'!D27", "='Historical_Financials'!D28"],
    ["='Historical_Financials'!E4", "='Historical_Financials'!E8", "='Historical_Financials'!E12", "='Historical_Financials'!E13", "='Historical_Financials'!E27", "='Historical_Financials'!E28"],
    ["='Historical_Financials'!F4", "='Historical_Financials'!F8", "='Historical_Financials'!F12", "='Historical_Financials'!F13", "='Historical_Financials'!F27", "='Historical_Financials'!F28"],
    ["='Historical_Financials'!G4", "='Historical_Financials'!G8", "='Historical_Financials'!G12", "='Historical_Financials'!G13", "='Historical_Financials'!G27", "='Historical_Financials'!G28"],
    ["='Historical_Financials'!H4", "='Historical_Financials'!H8", "='Historical_Financials'!H12", "='Historical_Financials'!H13", "='Historical_Financials'!H27", "='Historical_Financials'!H28"],
    ["='Historical_Financials'!I4", "='Historical_Financials'!I8", "='Historical_Financials'!I12", "='Historical_Financials'!I13", "='Historical_Financials'!I27", "='Historical_Financials'!I28"],
    ["='Historical_Financials'!J4", "='Historical_Financials'!J8", "='Historical_Financials'!J12", "='Historical_Financials'!J13", "='Historical_Financials'!J27", "='Historical_Financials'!J28"],
    ["='Historical_Financials'!K4", "='Historical_Financials'!K8", "='Historical_Financials'!K12", "='Historical_Financials'!K13", "='Historical_Financials'!K27", "='Historical_Financials'!K28"],
    ["='Historical_Financials'!L4", "='Historical_Financials'!L8", "='Historical_Financials'!L12", "='Historical_Financials'!L13", "='Historical_Financials'!L27", "='Historical_Financials'!L28"],
  ];
  linkFormula(s.getRange("A13:F23"));
  s.getRange("B13:C23").setNumberFormat(formats.currency);
  s.getRange("E13:E23").setNumberFormat(formats.currency);
  s.getRange("D13:D23").setNumberFormat(formats.percent);
  s.getRange("F13:F23").setNumberFormat(formats.percent);

  section(s, "H35:M35", "Chart Data");
  s.getRange("H36:J47").values = [
    ["Year", "EBITDA Margin", "FCF Margin"],
    [null, null, null],
    [null, null, null],
    [null, null, null],
    [null, null, null],
    [null, null, null],
    [null, null, null],
    [null, null, null],
    [null, null, null],
    [null, null, null],
    [null, null, null],
    [null, null, null],
  ];
  s.getRange("H37:J47").formulas = [
    ["=A13", "=D13", "=F13"],
    ["=A14", "=D14", "=F14"],
    ["=A15", "=D15", "=F15"],
    ["=A16", "=D16", "=F16"],
    ["=A17", "=D17", "=F17"],
    ["=A18", "=D18", "=F18"],
    ["=A19", "=D19", "=F19"],
    ["=A20", "=D20", "=F20"],
    ["=A21", "=D21", "=F21"],
    ["=A22", "=D22", "=F22"],
    ["=A23", "=D23", "=F23"],
  ];
  s.getRange("L36:M47").values = [
    ["Year", "Free Cash Flow"],
    [null, null],
    [null, null],
    [null, null],
    [null, null],
    [null, null],
    [null, null],
    [null, null],
    [null, null],
    [null, null],
    [null, null],
    [null, null],
  ];
  s.getRange("L37:M47").formulas = [
    ["=A13", "=E13"],
    ["=A14", "=E14"],
    ["=A15", "=E15"],
    ["=A16", "=E16"],
    ["=A17", "=E17"],
    ["=A18", "=E18"],
    ["=A19", "=E19"],
    ["=A20", "=E20"],
    ["=A21", "=E21"],
    ["=A22", "=E22"],
    ["=A23", "=E23"],
  ];
  header(s.getRange("H36:J36"));
  header(s.getRange("L36:M36"));
  s.getRange("I37:J47").setNumberFormat(formats.percent);
  s.getRange("M37:M47").setNumberFormat(formats.currency);

  const revenueChart = s.charts.add("line", s.getRange("A12:C23"));
  revenueChart.title = "Revenue and EBITDA Trend ($mm)";
  revenueChart.hasLegend = true;
  revenueChart.xAxis = { axisType: "textAxis", textStyle: { fontSize: 9 } };
  revenueChart.yAxis = { numberFormatCode: "$#,##0" };
  revenueChart.setPosition("H3", "O17");

  const marginChart = s.charts.add("line", s.getRange("H36:J47"));
  marginChart.title = "Margin Trend (%)";
  marginChart.hasLegend = true;
  marginChart.xAxis = { axisType: "textAxis", textStyle: { fontSize: 9 } };
  marginChart.yAxis = { numberFormatCode: "0%" };
  marginChart.setPosition("H19", "O33");

  const fcfChart = s.charts.add("bar", s.getRange("L36:M47"));
  fcfChart.title = "Free Cash Flow ($mm)";
  fcfChart.hasLegend = false;
  fcfChart.xAxis = { axisType: "textAxis", textStyle: { fontSize: 9 } };
  fcfChart.yAxis = { numberFormatCode: "$#,##0" };
  fcfChart.setPosition("A27", "F41");
}

async function renderPreviews() {
  const previewSheets = [
    "Cover",
    "Dashboard",
    "Assumptions",
    "Historical_Financials",
    "Ratios",
    "Valuation_DCF",
    "Sensitivity",
    "Checks",
    "Sources",
  ];
  for (const sheetName of previewSheets) {
    const preview = await workbook.render({
      sheetName,
      autoCrop: "all",
      scale: 1,
      format: "png",
    });
    await fs.writeFile(
      path.join(outputDir, `${sheetName}.png`),
      new Uint8Array(await preview.arrayBuffer()),
    );
  }
}

async function verifyWorkbook() {
  const dashboard = await workbook.inspect({
    kind: "table",
    range: "Dashboard!A1:F24",
    include: "values,formulas",
    tableMaxRows: 24,
    tableMaxCols: 6,
    maxChars: 6000,
  });
  console.log("DASHBOARD_INSPECT");
  console.log(dashboard.ndjson);

  const checks = await workbook.inspect({
    kind: "table",
    range: "Checks!A3:G13",
    include: "values,formulas",
    tableMaxRows: 13,
    tableMaxCols: 7,
    maxChars: 6000,
  });
  console.log("CHECKS_INSPECT");
  console.log(checks.ndjson);

  const errors = await workbook.inspect({
    kind: "match",
    searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
    options: { useRegex: true, maxResults: 300 },
    summary: "final formula error scan",
    maxChars: 6000,
  });
  console.log("ERROR_SCAN");
  console.log(errors.ndjson);
}

async function main() {
  await fs.mkdir(outputDir, { recursive: true });
  try {
    workbook.comments.setSelf({ displayName: "User" });
  } catch {
    // Comment author setup is optional if the runtime does not expose comments.
  }

  hideGridlines();
  await buildCover();
  await buildAssumptions();
  await buildFinancials();
  await buildRatios();
  await buildValuation();
  await buildSensitivity();
  await buildChecks();
  await buildSources();
  await buildDashboard();
  await verifyWorkbook();
  await renderPreviews();

  const output = await SpreadsheetFile.exportXlsx(workbook);
  await output.save(path.join(outputDir, "Company_Financial_Analysis_Template.xlsx"));
  console.log(path.join(outputDir, "Company_Financial_Analysis_Template.xlsx"));
}

await main();

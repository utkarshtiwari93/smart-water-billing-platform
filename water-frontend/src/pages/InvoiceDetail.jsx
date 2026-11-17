import { useId, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { downloadFile, get } from '../api.js';
import { useAuth } from '../auth.jsx';
import { dateStr, dateTimeStr, litres, money, number } from '../fmt.js';
import { Badge, ErrorBox, PageHeader, Spinner, useAsync } from '../ui.jsx';

/**
 * Invoice detail
 * ----------------------------------------------------------------------------
 * UI-only refresh. The data logic is unchanged:
 *  - Same imports and same default export (InvoiceDetail).
 *  - Same request: GET /api/v1/invoices/{id}.
 *  - Same PDF download: downloadFile(`/api/v1/invoices/{id}/pdf`, `{invoiceNumber}.pdf`).
 *  - Same Back target (admin: the billing cycle, resident: /my-invoices) and the
 *    same "Print / Save as PDF" button (window.print()).
 *
 * Printing: the toolbar keeps the shared "no-print" class and the invoice keeps
 * the "invoice-page" and "invoice" classes, so any global print rules you already
 * have still find them. This file also adds its own print rules (below) that hide
 * the on-screen help and keep table rows from splitting across pages.
 *
 * Styling is injected by this file and every selector starts with .ivd-page, so
 * no other page is affected. Colours and type match the rest of the app.
 *
 * Change BRAND_NAME / BRAND_TAGLINE below if the invoice header should say
 * something else (the previous version said "Smart Water Platform").
 */

const BRAND_NAME = 'AquaFlow';
const BRAND_TAGLINE = 'Intelligent Water Usage Monitoring & Automated Billing';

const TYPE_LABEL = {
  WATER_TIER1: 'Water consumption (tier 1)',
  WATER_TIER2: 'Water consumption (tier 2)',
  SHARED_AREA: 'Shared common area utility',
  ADJUSTMENT: 'Adjustment / Surcharge',
};

const KNOWN_TYPES = Object.keys(TYPE_LABEL);

const GUIDE = [
  { term: 'Water consumption (tier 1 and tier 2)', text: 'Water is charged in slabs. Usage up to the first slab limit is billed at the tier 1 rate, and usage beyond it at the tier 2 rate. Quantities are shown in kilolitres (1 kL = 1,000 litres).' },
  { term: 'Shared common area utility', text: 'Your household’s share of common costs, such as water bought in bulk for the whole apartment. Administrators can review how each share was allocated.' },
  { term: 'Adjustment / Surcharge', text: 'A one-off correction or extra charge, shown as its own line so it is easy to spot.' },
  { term: 'Total payable', text: 'The sum of all lines above. The tariff plan in force when the cycle was finalized is the one used for the bill.' },
];

/* ------------------------------------------------------------------ */
/*  Small helpers                                                      */
/* ------------------------------------------------------------------ */

function sumAmount(list) {
  return list.reduce((total, item) => total + (Number(item.amount) || 0), 0);
}

function sumQuantity(list) {
  return list.reduce((total, item) => total + (Number(item.quantity) || 0), 0);
}

function Mark() {
  // Unique gradient id so the mark never collides with other SVGs on the page.
  const gradientId = useId();
  return (
    <svg viewBox="0 0 40 48" fill="none" aria-hidden="true">
      <path d="M20 2C15 10 5 21 5 30a15 15 0 0 0 30 0C35 21 25 10 20 2Z" fill={`url(#${gradientId})`} />
      <path d="M12 30a8 8 0 0 0 8 8" stroke="white" strokeWidth="2.5" strokeLinecap="round" />
      <defs>
        <linearGradient id={gradientId} x1="7" y1="4" x2="34" y2="43" gradientUnits="userSpaceOnUse">
          <stop stopColor="#19E5FA" />
          <stop offset="1" stopColor="#0875CE" />
        </linearGradient>
      </defs>
    </svg>
  );
}

/* ------------------------------------------------------------------ */
/*  Page                                                               */
/* ------------------------------------------------------------------ */

export default function InvoiceDetail() {
  const { id } = useParams();
  const { isAdmin } = useAuth();
  const { data: inv, loading, error } = useAsync(() => get(`/api/v1/invoices/${id}`), [id]);
  const [pdfLoading, setPdfLoading] = useState(false);
  const [pdfError, setPdfError] = useState(null);

  const downloadInvoice = async () => {
    setPdfLoading(true);
    setPdfError(null);
    try {
      await downloadFile(`/api/v1/invoices/${id}/pdf`, `${inv.invoiceNumber}.pdf`);
    } catch (downloadFailure) {
      setPdfError(downloadFailure);
    } finally {
      setPdfLoading(false);
    }
  };

  if (loading) return <Spinner />;
  if (error) return <ErrorBox error={error} />;
  if (!inv) return <ErrorBox error={new Error('Invoice not found.')} />;

  const back = isAdmin ? `/admin/cycles/${inv.cycleId}` : '/my-invoices';
  const items = Array.isArray(inv.items) ? inv.items : [];

  // Summary lines are calculated here for readability. The total shown is always inv.totalAmount.
  const waterItems = items.filter((it) => it.type === 'WATER_TIER1' || it.type === 'WATER_TIER2');
  const sharedItems = items.filter((it) => it.type === 'SHARED_AREA');
  const adjustmentItems = items.filter((it) => it.type === 'ADJUSTMENT');
  const otherItems = items.filter((it) => !KNOWN_TYPES.includes(it.type));
  const summaryRows = [
    { label: 'Water consumption', list: waterItems },
    { label: 'Shared common area utility', list: sharedItems },
    { label: 'Adjustments and surcharges', list: adjustmentItems },
    { label: 'Other charges', list: otherItems },
  ].filter((row) => row.list.length > 0);

  // Tier split for the usage bar.
  const tier1Qty = sumQuantity(items.filter((it) => it.type === 'WATER_TIER1'));
  const tier2Qty = sumQuantity(items.filter((it) => it.type === 'WATER_TIER2'));
  const tierTotal = tier1Qty + tier2Qty;
  const tier1Pct = tierTotal > 0 ? (tier1Qty / tierTotal) * 100 : 0;

  return (
    <div className="invoice-page ivd-page">
      <style>{STYLES}</style>

      <div className="no-print ivd-noprint">
        <PageHeader
          title={`Invoice #${inv.invoiceNumber}`}
          subtitle={`Billing statement for Flat ${inv.flatNumber}`}
          actions={(
            <>
              <Link className="btn btn-ghost" to={back}>
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" style={{ marginRight: 4 }}>
                  <line x1="19" y1="12" x2="5" y2="12" />
                  <polyline points="12 19 5 12 12 5" />
                </svg>
                Back
              </Link>
              <button className="btn btn-primary" onClick={() => window.print()}>
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" style={{ marginRight: 4 }}>
                  <polyline points="6 9 6 2 18 2 18 9" />
                  <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2" />
                  <rect x="6" y="14" width="12" height="8" />
                </svg>
                Print / Save as PDF
              </button>
              <button className="btn btn-primary" disabled={pdfLoading} onClick={downloadInvoice}>
                {pdfLoading ? 'Preparing PDF...' : 'Download PDF'}
              </button>
            </>
          )}
        />
        <ErrorBox error={pdfError} />
      </div>

      <article className="invoice ivd-doc" aria-label={`Invoice ${inv.invoiceNumber}`}>
        <header className="ivd-head">
          <div className="ivd-brand">
            <span className="ivd-mark"><Mark /></span>
            <div>
              <strong>{BRAND_NAME}</strong>
              <span>{BRAND_TAGLINE}</span>
            </div>
          </div>
          <div className="ivd-ident">
            <span className="ivd-doc-title">Invoice</span>
            <strong className="ivd-number">{inv.invoiceNumber}</strong>
            <span className="ivd-issued">Issued on {dateTimeStr(inv.issuedAt)}</span>
          </div>
        </header>

        <section className="ivd-overview" aria-label="Invoice summary">
          <dl className="ivd-meta">
            <div>
              <dt>Household / Flat</dt>
              <dd>{inv.flatNumber}</dd>
            </div>
            <div>
              <dt>Billing period</dt>
              <dd>{dateStr(inv.periodStart)} to {dateStr(inv.periodEnd)}</dd>
            </div>
            <div>
              <dt>Total consumption</dt>
              <dd className="is-accent">{litres(inv.consumptionLitres)}</dd>
            </div>
          </dl>
          <div className="ivd-due">
            <span>Total payable</span>
            <strong>{money(inv.totalAmount)}</strong>
            <small>Payment instructions are included in the downloaded PDF.</small>
          </div>
        </section>

        {tierTotal > 0 && (
          <section className="ivd-tiers" aria-label="How your water use was charged">
            <h2>How your water use was charged</h2>
            <div className="ivd-bar" role="img" aria-label={`Tier 1: ${number(tier1Qty)} kL, tier 2: ${number(tier2Qty)} kL`}>
              <span className="is-t1" style={{ width: `${tier1Pct}%` }} />
              <span className="is-t2" style={{ width: `${100 - tier1Pct}%` }} />
            </div>
            <ul className="ivd-legend">
              <li><i className="is-t1" />Tier 1: {number(tier1Qty)} kL</li>
              <li><i className="is-t2" />Tier 2: {number(tier2Qty)} kL</li>
            </ul>
          </section>
        )}

        <section className="ivd-charges" aria-label="Charges">
          <h2>Charges</h2>
          <div className="ivd-table-scroll">
            <table className="ivd-table">
              <caption className="ivd-sr">Invoice line items</caption>
              <thead>
                <tr>
                  <th scope="col">Item type</th>
                  <th scope="col">Description</th>
                  <th scope="col" className="is-num">Quantity (kL)</th>
                  <th scope="col" className="is-num">Unit rate</th>
                  <th scope="col" className="is-num">Amount</th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr className="ivd-empty-row"><td colSpan="5">No line items are recorded on this invoice.</td></tr>
                ) : items.map((it, i) => (
                  <tr key={i}>
                    <td data-label="Item type"><Badge tone={it.type === 'ADJUSTMENT' ? 'amber' : 'blue'}>{TYPE_LABEL[it.type] || it.type}</Badge></td>
                    <td data-label="Description">{it.description}</td>
                    <td data-label="Quantity (kL)" className="is-num">{it.quantity == null ? '-' : number(it.quantity)}</td>
                    <td data-label="Unit rate" className="is-num">{it.unitRate == null ? '-' : money(it.unitRate)}</td>
                    <td data-label="Amount" className="is-num"><strong>{money(it.amount)}</strong></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <dl className="ivd-totals">
            {summaryRows.map((row) => (
              <div key={row.label}><dt>{row.label}</dt><dd>{money(sumAmount(row.list))}</dd></div>
            ))}
            <div className="is-total"><dt>Total payable amount</dt><dd>{money(inv.totalAmount)}</dd></div>
          </dl>
        </section>

        <p className="ivd-disclaimer">
          This is an official computer-generated utility statement. All consumption data reflects recorded household meter intervals and approved society tariff formulas.
        </p>
      </article>

      {/* On-screen help: hidden when printing so the invoice stays clean */}
      <section className="no-print ivd-noprint ivd-guide" aria-labelledby="ivd-guide-title">
        <h2 id="ivd-guide-title">How to read this invoice</h2>
        <dl>
          {GUIDE.map((item) => (
            <div key={item.term}><dt>{item.term}</dt><dd>{item.text}</dd></div>
          ))}
        </dl>
        <p className="ivd-help">
          {isAdmin
            ? 'To review how this bill was built, open the billing cycle it belongs to using the Back button.'
            : 'If something on this invoice looks wrong, contact your society administrator and quote the invoice number.'}
        </p>
      </section>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Styles: every selector starts with .ivd-page                       */
/* ------------------------------------------------------------------ */

const STYLES = `
.ivd-page {
  --ivd-ink: #0b2540;
  --ivd-text: #35506a;
  --ivd-muted: #5b7186;
  --ivd-tide: #0a6ea8;
  --ivd-deep: #07496f;
  --ivd-foam: #e8f3f9;
  --ivd-paper: #f5f9fb;
  --ivd-line: #d8e5ed;
  --ivd-brass: #a9681d;
  --ivd-display: "Iowan Old Style", "Palatino Linotype", Palatino, "Book Antiqua", Georgia, serif;
  color: var(--ivd-ink);
}
.ivd-page *, .ivd-page *::before, .ivd-page *::after { box-sizing: border-box; }
.ivd-page :where(h2, p, ul, dl, dd, figure) { margin: 0; }
.ivd-page :where(ul) { padding: 0; list-style: none; }
.ivd-page svg { flex: none; }
.ivd-page .ivd-sr { position: absolute; width: 1px; height: 1px; margin: -1px; padding: 0; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; border: 0; }

/* The invoice sheet (overrides any older global .invoice styling) */
.ivd-page .ivd-doc.invoice { width: 100%; max-width: 920px; margin: 0 auto; padding: 40px; border: 1px solid var(--ivd-line); border-radius: 14px; background: #fff; box-shadow: 0 30px 60px -40px rgba(11, 37, 64, .35); color: var(--ivd-ink); }

.ivd-page .ivd-head { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: flex-start; gap: 20px 32px; padding-bottom: 24px; border-bottom: 2px solid var(--ivd-ink); }
.ivd-page .ivd-brand { display: flex; align-items: center; gap: 12px; min-width: 0; }
.ivd-page .ivd-mark { width: 36px; height: 43px; display: grid; place-items: center; }
.ivd-page .ivd-mark svg { width: 100%; height: 100%; }
.ivd-page .ivd-brand strong { display: block; font-size: 22px; font-weight: 800; letter-spacing: -.6px; line-height: 1.1; }
.ivd-page .ivd-brand span { display: block; margin-top: 3px; max-width: 320px; color: var(--ivd-muted); font-size: 12.5px; line-height: 1.45; }
.ivd-page .ivd-ident { display: flex; flex-direction: column; align-items: flex-end; text-align: right; }
.ivd-page .ivd-doc-title { color: var(--ivd-muted); font-family: var(--ivd-display); font-size: 20px; font-style: italic; }
.ivd-page .ivd-number { margin-top: 2px; font-family: var(--ivd-display); font-size: 28px; font-weight: 600; letter-spacing: -.01em; line-height: 1.15; overflow-wrap: anywhere; }
.ivd-page .ivd-issued { margin-top: 4px; color: var(--ivd-muted); font-size: 13px; }

.ivd-page .ivd-overview { display: grid; grid-template-columns: minmax(0, 1fr) 260px; gap: 28px; align-items: stretch; padding: 24px 0; border-bottom: 1px solid var(--ivd-line); }
.ivd-page .ivd-meta { display: grid; grid-template-columns: 1fr 1.6fr 1.2fr; gap: 20px; align-content: center; }
.ivd-page .ivd-meta dt { color: var(--ivd-muted); font-size: 13px; font-weight: 600; }
.ivd-page .ivd-meta dd { margin-top: 4px; font-size: 16px; font-weight: 700; line-height: 1.4; overflow-wrap: anywhere; }
.ivd-page .ivd-meta dd.is-accent { color: var(--ivd-tide); }
.ivd-page .ivd-due { padding: 18px 20px; border-radius: 12px; background: var(--ivd-foam); border: 1px solid #cfe6f1; display: flex; flex-direction: column; justify-content: center; gap: 4px; }
.ivd-page .ivd-due span { color: var(--ivd-deep); font-size: 13px; font-weight: 700; }
.ivd-page .ivd-due strong { font-family: var(--ivd-display); font-size: 34px; font-weight: 600; line-height: 1.1; letter-spacing: -.02em; font-variant-numeric: tabular-nums; }
.ivd-page .ivd-due small { color: var(--ivd-muted); font-size: 12px; line-height: 1.5; }

.ivd-page .ivd-tiers { padding: 22px 0 4px; }
.ivd-page .ivd-tiers h2, .ivd-page .ivd-charges h2 { font-family: var(--ivd-display); font-size: 20px; font-weight: 600; color: var(--ivd-ink); }
.ivd-page .ivd-bar { display: flex; height: 12px; margin-top: 12px; border-radius: 999px; overflow: hidden; background: var(--ivd-line); }
.ivd-page .ivd-bar span { display: block; height: 100%; }
.ivd-page .ivd-bar .is-t1, .ivd-page .ivd-legend .is-t1 { background: var(--ivd-tide); }
.ivd-page .ivd-bar .is-t2, .ivd-page .ivd-legend .is-t2 { background: var(--ivd-brass); }
.ivd-page .ivd-legend { display: flex; flex-wrap: wrap; gap: 6px 20px; margin-top: 10px; color: var(--ivd-text); font-size: 13px; }
.ivd-page .ivd-legend i { display: inline-block; width: 9px; height: 9px; margin-right: 7px; border-radius: 50%; }

.ivd-page .ivd-charges { padding-top: 26px; }
.ivd-page .ivd-table-scroll { margin-top: 12px; }
.ivd-page .ivd-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.ivd-page .ivd-table th, .ivd-page .ivd-table td { padding: 13px 10px; border-bottom: 1px solid var(--ivd-line); text-align: left; vertical-align: top; background: transparent; }
.ivd-page .ivd-table thead th { border-bottom: 2px solid var(--ivd-ink); color: var(--ivd-muted); font-size: 12.5px; font-weight: 700; }
.ivd-page .ivd-table .is-num { text-align: right; font-variant-numeric: tabular-nums; white-space: nowrap; }
.ivd-page .ivd-table tbody td:first-child, .ivd-page .ivd-table thead th:first-child { padding-left: 0; }
.ivd-page .ivd-table tbody td:last-child, .ivd-page .ivd-table thead th:last-child { padding-right: 0; }
.ivd-page .ivd-empty-row td { padding: 24px 0; color: var(--ivd-muted); text-align: center; }
.ivd-page .ivd-table tr { break-inside: avoid; }

.ivd-page .ivd-totals { width: min(100%, 360px); margin: 18px 0 0 auto; }
.ivd-page .ivd-totals > div { display: flex; justify-content: space-between; gap: 16px; padding: 7px 0; color: var(--ivd-text); font-size: 14px; }
.ivd-page .ivd-totals dd { font-variant-numeric: tabular-nums; }
.ivd-page .ivd-totals .is-total { margin-top: 8px; padding-top: 14px; border-top: 2px solid var(--ivd-ink); color: var(--ivd-ink); font-size: 16px; font-weight: 800; }
.ivd-page .ivd-totals .is-total dd { font-family: var(--ivd-display); font-size: 24px; font-weight: 600; }

.ivd-page .ivd-disclaimer { margin-top: 30px; padding: 14px 16px; border-left: 3px solid var(--ivd-tide); background: var(--ivd-paper); color: var(--ivd-muted); font-size: 12.5px; line-height: 1.65; }

/* On-screen guide */
.ivd-page .ivd-guide { width: 100%; max-width: 920px; margin: 24px auto 0; padding: 28px; border: 1px solid var(--ivd-line); border-radius: 14px; background: #fff; }
.ivd-page .ivd-guide h2 { font-family: var(--ivd-display); font-size: 24px; font-weight: 600; letter-spacing: -.01em; color: var(--ivd-ink); }
.ivd-page .ivd-guide dl { margin-top: 14px; }
.ivd-page .ivd-guide dl > div { padding: 13px 0; border-top: 1px solid var(--ivd-line); }
.ivd-page .ivd-guide dt { font-size: 14.5px; font-weight: 700; color: var(--ivd-ink); }
.ivd-page .ivd-guide dd { margin-top: 3px; color: var(--ivd-text); font-size: 14px; line-height: 1.7; }
.ivd-page .ivd-help { margin-top: 6px; padding-top: 14px; border-top: 1px solid var(--ivd-line); color: var(--ivd-muted); font-size: 13.5px; line-height: 1.6; }

/* Tablet */
@media (max-width: 820px) {
  .ivd-page .ivd-doc.invoice { padding: 28px 22px; }
  .ivd-page .ivd-overview { grid-template-columns: minmax(0, 1fr); gap: 20px; }
  .ivd-page .ivd-meta { grid-template-columns: 1fr 1fr; }
  .ivd-page .ivd-ident { align-items: flex-start; text-align: left; }
}

/* Phone: line items become stacked rows */
@media (max-width: 640px) {
  .ivd-page .ivd-doc.invoice { padding: 22px 16px; border-radius: 12px; }
  .ivd-page .ivd-meta { grid-template-columns: minmax(0, 1fr); gap: 14px; }
  .ivd-page .ivd-number { font-size: 24px; }
  .ivd-page .ivd-due strong { font-size: 30px; }
  .ivd-page .ivd-table thead { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
  .ivd-page .ivd-table, .ivd-page .ivd-table tbody, .ivd-page .ivd-table tr, .ivd-page .ivd-table td { display: block; width: 100%; }
  .ivd-page .ivd-table tr { padding: 12px 0; border-bottom: 1px solid var(--ivd-line); }
  .ivd-page .ivd-table td { display: flex; justify-content: space-between; gap: 16px; padding: 5px 0; border: 0; text-align: right; }
  .ivd-page .ivd-table td::before { content: attr(data-label); flex: none; color: var(--ivd-muted); font-size: 12.5px; font-weight: 600; text-align: left; }
  .ivd-page .ivd-table .is-num { white-space: normal; }
  .ivd-page .ivd-empty-row td { display: block; text-align: center; }
  .ivd-page .ivd-empty-row td::before { content: none; }
  .ivd-page .ivd-totals { width: 100%; }
  .ivd-page .ivd-guide { padding: 20px 16px; }
}

/* Print: keep the sheet clean and the table intact (must stay last) */
@media print {
  .ivd-page { color: #000; }
  .ivd-page .ivd-noprint { display: none !important; }
  .ivd-page .ivd-doc.invoice { max-width: none; margin: 0; padding: 0; border: 0; border-radius: 0; box-shadow: none; }
  .ivd-page .ivd-overview { grid-template-columns: minmax(0, 1fr) 240px; gap: 20px; }
  .ivd-page .ivd-meta { grid-template-columns: 1fr 1.6fr 1.2fr; }
  .ivd-page .ivd-ident { align-items: flex-end; text-align: right; }
  .ivd-page .ivd-table thead { position: static; width: auto; height: auto; overflow: visible; clip: auto; }
  .ivd-page .ivd-table { display: table; }
  .ivd-page .ivd-table tbody { display: table-row-group; }
  .ivd-page .ivd-table tr { display: table-row; padding: 0; }
  .ivd-page .ivd-table td { display: table-cell; width: auto; padding: 13px 10px; border-bottom: 1px solid var(--ivd-line); text-align: left; }
  .ivd-page .ivd-table td.is-num { text-align: right; white-space: nowrap; }
  .ivd-page .ivd-table td::before { content: none; }
  .ivd-page .ivd-table tbody td:first-child { padding-left: 0; }
  .ivd-page .ivd-table tbody td:last-child { padding-right: 0; }
  .ivd-page .ivd-totals { width: 340px; }
  .ivd-page .ivd-doc, .ivd-page .ivd-doc * { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  .ivd-page .ivd-totals, .ivd-page .ivd-disclaimer, .ivd-page .ivd-overview { break-inside: avoid; }
}
`;
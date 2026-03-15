// import React, { useMemo, useState } from 'react';
// import { convertIdeaToProposal, type ProposalCreateRequestDTO } from '../../utils/proposalApi';

// type Props = {
//   ideaId: number;            // The accepted idea to convert
//   onSuccess?: (proposalId: number) => void; // Optional callback after success
// };

// export default function ProposalCreateForm({ ideaId, onSuccess }: Props) {
//   const [form, setForm] = useState({
//     budget: '',
//     timeLineStart: '',
//     timeLineEnd: '',
//     objectives: '',
//     objectivesProof: '',
//   });

//   const [busy, setBusy] = useState(false);
//   const [err, setErr] = useState('');
//   const [ok, setOk] = useState('');

//   const validDates = useMemo(() => {
//     if (!form.timeLineStart || !form.timeLineEnd) return true;
//     return new Date(form.timeLineEnd) >= new Date(form.timeLineStart);
//   }, [form.timeLineStart, form.timeLineEnd]);

//   const canSubmit = useMemo(() => {
//     const budget = Number(form.budget);
//     return (
//       form.objectives.trim().length > 0 &&
//       form.timeLineStart !== '' &&
//       form.timeLineEnd !== '' &&
//       !Number.isNaN(budget) &&
//       budget >= 0 &&
//       validDates
//     );
//   }, [form, validDates]);

//   const onChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
//     const { name, value } = e.target;
//     setForm((f) => ({ ...f, [name]: value }));
//   };

//   const submit = async (e: React.FormEvent) => {
//     e.preventDefault();
//     if (!canSubmit) return;
//     setBusy(true);
//     setErr('');
//     setOk('');
//     try {
//       const payload: ProposalCreateRequestDTO = {
//         budget: Number(form.budget),
//         timeLineStart: form.timeLineStart,
//         timeLineEnd: form.timeLineEnd,
//         objectives: form.objectives.trim(),
//         objectivesProof: form.objectivesProof?.trim() || undefined,
//       };
//       const resp = await convertIdeaToProposal(ideaId, payload);
//       setOk('Proposal created successfully.');
//       if (onSuccess && resp?.proposalId) onSuccess(resp.proposalId);
//       // optional: reset form
//       // setForm({ budget: '', timeLineStart: '', timeLineEnd: '', objectives: '', objectivesProof: '' });
//     } catch (e: any) {
//       setErr(e?.message || 'Failed to create proposal');
//     } finally {
//       setBusy(false);
//     }
//   };

//   return (
//     <div className="card">
//       <div className="card-header">Create Proposal (Idea #{ideaId})</div>
//       <div className="card-body">
//         {err && <div className="alert alert-danger">{err}</div>}
//         {ok && <div className="alert alert-success">{ok}</div>}

//         <form onSubmit={submit}>
//           <div className="row g-3">
//             <div className="col-md-4">
//               <label className="form-label">Budget</label>
//               <input
//                 className={`form-control ${form.budget !== '' && Number(form.budget) < 0 ? 'is-invalid' : ''}`}
//                 type="number"
//                 name="budget"
//                 min={0}
//                 step="0.01"
//                 value={form.budget}
//                 onChange={onChange}
//                 required
//               />
//               <div className="invalid-feedback">Budget cannot be negative.</div>
//             </div>
//             <div className="col-md-4">
//               <label className="form-label">Timeline Start</label>
//               <input
//                 className="form-control"
//                 type="date"
//                 name="timeLineStart"
//                 value={form.timeLineStart}
//                 onChange={onChange}
//                 required
//               />
//             </div>
//             <div className="col-md-4">
//               <label className="form-label">Timeline End</label>
//               <input
//                 className={`form-control ${form.timeLineEnd && !validDates ? 'is-invalid' : ''}`}
//                 type="date"
//                 name="timeLineEnd"
//                 value={form.timeLineEnd}
//                 onChange={onChange}
//                 required
//               />
//               <div className="invalid-feedback">End date must be on or after start date.</div>
//             </div>

//             <div className="col-12">
//               <label className="form-label">Objectives</label>
//               <textarea
//                 className="form-control"
//                 name="objectives"
//                 rows={3}
//                 value={form.objectives}
//                 onChange={onChange}
//                 required
//               />
//             </div>

//             <div className="col-12">
//               <label className="form-label">Proof for Objectives (optional)</label>
//               <textarea
//                 className="form-control"
//                 name="objectivesProof"
//                 rows={2}
//                 value={form.objectivesProof}
//                 onChange={onChange}
//               />
//             </div>
//           </div>

//           <div className="mt-3 d-flex gap-2">
//             <button className="btn btn-primary" type="submit" disabled={!canSubmit || busy}>
//               {busy ? 'Creating…' : 'Create Proposal'}
//             </button>
//             <button
//               className="btn btn-outline-secondary"
//               type="button"
//               onClick={() =>
//                 setForm({ budget: '', timeLineStart: '', timeLineEnd: '', objectives: '', objectivesProof: '' })
//               }
//               disabled={busy}
//             >
//               Reset
//             </button>
//           </div>
//         </form>
//       </div>
//     </div>
//   );
// }

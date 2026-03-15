import { useEffect, useRef, useState } from "react";
import {
    deleteReport, generateReport, getAllDept, getAllCategories,
    getProfile, getReportList,
    type IReport, type IViewCategory,
} from "../../utils/analyticsApi";
import { type IViewDept } from "../../utils/reviewerAssignmentApi";
import { useAuth } from "../../utils/authContext";
import { useShowToast } from "../../hooks/useShowToast";
import ConfirmationModal from "../../components/ConfirmationModal/ConfirmationModal";
import { BarChart2, Plus, Trash2, X, FileText } from "lucide-react";
import "./Report.css";

// -- Validation state shape ---------------------------------------
interface FormErrors {
    scope?: string;
    scopeId?: string;
    period?: string;
}

const ReportCreationAndDisplay = () => {
    const { token } = useAuth();
    const toast = useShowToast();

    // -- Report list --------------------------------------------
    const [reportList, setReportList] = useState<IReport[]>([]);
    const [listLoading, setListLoading] = useState(false);
    const [selectedPeriod, setSelectedPeriod] = useState("");

    // -- Logged-in user -----------------------------------------
    const [loggedUserId, setLoggedUserId] = useState<number | null>(null);

    const loadProfile = async () => {
        try {
            const { data } = await getProfile();
            const rawId = (data as any)?.userId ?? (data as any)?.id;
            const idNum = Number(rawId);
            if (!Number.isNaN(idNum)) {
                setLoggedUserId(idNum);
                localStorage.setItem("user-profile", JSON.stringify({ userId: idNum }));
            }
        } catch (e) {
            console.error("loadProfile failed", e);
        }
    };

    useEffect(() => { if (token) loadProfile(); }, [token]);

    // -- Delete confirmation ------------------------------------
    const [deleteTarget, setDeleteTarget] = useState<number | null>(null);

    const doDelete = () => {
        if (deleteTarget === null) return;
        const id = deleteTarget;
        setDeleteTarget(null);
        deleteReport(id)
            .then(() => {
                toast.success("Report deleted successfully.");
                if (selectedPeriod) {
                    const [y, m] = selectedPeriod.split("-");
                    fetchReports(Number(y), Number(m));
                }
            })
            .catch(err => {
                console.error(err);
                toast.error("Failed to delete report.");
            });
    };

    // -- Fetch reports ------------------------------------------
    const fetchReports = (year: number, month: number) => {
        setListLoading(true);
        getReportList(year, month)
            .then(res => setReportList(res.data))
            .catch(err => console.error(err))
            .finally(() => setListLoading(false));
    };

    const handleDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const val = e.target.value;
        setSelectedPeriod(val);
        if (val) {
            const [year, month] = val.split("-");
            fetchReports(Number(year), Number(month));
        } else {
            setReportList([]);
        }
    };

    // -- Modal state --------------------------------------------
    const [showModal, setShowModal] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [formErrors, setFormErrors] = useState<FormErrors>({});
    const [generateReportObj, setGenerateReportObj] = useState({
        scope: "",
        scopeId: 0,
        userId: 0,
        year: "null",
        month: "null",
    });
    const modalPeriodRef = useRef<HTMLInputElement>(null);

    const [categories, setCategories] = useState<IViewCategory[]>([]);
    const [departments, setDepartments] = useState<IViewDept[]>([]);

    const openModal = () => {
        setGenerateReportObj({ scope: "", scopeId: 0, userId: loggedUserId ?? 0, year: "null", month: "null" });
        setFormErrors({});
        setShowModal(true);
    };

    const closeModal = () => {
        setShowModal(false);
        setFormErrors({});
    };

    // -- Form validation ----------------------------------------
    const validate = (): FormErrors => {
        const errors: FormErrors = {};
        if (!generateReportObj.scope)
            errors.scope = "Please select a report scope.";
        if ((generateReportObj.scope === "DEPARTMENT" || generateReportObj.scope === "CATEGORY") && !generateReportObj.scopeId)
            errors.scopeId = `Please select a ${generateReportObj.scope === "DEPARTMENT" ? "department" : "category"}.`;
        if (generateReportObj.scope === "PERIOD" && (generateReportObj.year === "null" || generateReportObj.month === "null"))
            errors.period = "Please select a month and year.";
        return errors;
    };

    // -- Scope change -------------------------------------------
    const onScopeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        const selectedScope = e.target.value;
        setGenerateReportObj(prev => ({ ...prev, scope: selectedScope, scopeId: 0, year: "null", month: "null" }));
        setFormErrors(prev => ({ ...prev, scope: undefined, scopeId: undefined, period: undefined }));

        if (selectedScope === "CATEGORY")
            getAllCategories().then(res => setCategories(res.data)).catch(console.error);
        else if (selectedScope === "DEPARTMENT")
            getAllDept().then(res => setDepartments(res.data)).catch(console.error);
    };

    const handleDeptChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        setGenerateReportObj(prev => ({ ...prev, scopeId: Number(e.target.value) }));
        setFormErrors(prev => ({ ...prev, scopeId: undefined }));
    };

    const handleCategoryChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        setGenerateReportObj(prev => ({ ...prev, scopeId: Number(e.target.value) }));
        setFormErrors(prev => ({ ...prev, scopeId: undefined }));
    };

    const handlePeriodChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const val = e.target.value;
        if (val) {
            const [y, m] = val.split("-");
            setGenerateReportObj(prev => ({ ...prev, year: y, month: m }));
            setFormErrors(prev => ({ ...prev, period: undefined }));
        } else {
            setGenerateReportObj(prev => ({ ...prev, year: "null", month: "null" }));
        }
    };

    // -- Submit -------------------------------------------------
    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        const errors = validate();
        if (Object.keys(errors).length > 0) {
            setFormErrors(errors);
            return;
        }
        setSubmitting(true);
        generateReport(generateReportObj)
            .then(() => {
                toast.success("Report generated successfully.");
                closeModal();
                if (selectedPeriod) {
                    const [year, month] = selectedPeriod.split("-");
                    fetchReports(Number(year), Number(month));
                }
            })
            .catch(err => {
                console.error(err);
                toast.error("Failed to generate report.");
            })
            .finally(() => setSubmitting(false));
    };

    // -- Scope badge helper -------------------------------------
    const scopeBadgeClass = (scope: string) => {
        if (scope === "DEPARTMENT") return "rpt-badge rpt-badge-dept";
        if (scope === "CATEGORY")   return "rpt-badge rpt-badge-cat";
        return "rpt-badge rpt-badge-period";
    };

    // -- Render -------------------------------------------------
    return (
        <div className="rpt-page">
            <div className="rpt-card">
                {/* Card title row */}
                <div className="rpt-card-header">
                    <h2 className="rpt-card-title">
                        <span className="rpt-card-title-icon">
                            <BarChart2 size={17} />
                        </span>
                        Reports
                    </h2>
                </div>

                {/* Controls: period filter + create button */}
                <div className="rpt-header-controls">
                    <div className="rpt-filter-group">
                        <span className="rpt-filter-label">Filter by Period</span>
                        <input
                            type="month"
                            className="rpt-filter-input"
                            value={selectedPeriod}
                            onChange={handleDateChange}
                        />
                    </div>

                    <button className="rpt-btn-primary" style={{ marginLeft: "auto" }} onClick={openModal}>
                        <Plus size={15} />
                        Create Report
                    </button>
                </div>

                {/* Report table */}
                <div>
                    {listLoading && (
                        <div className="rpt-loading">
                            <span
                                className="spinner-border spinner-border-sm text-primary"
                                role="status"
                                style={{ width: "1.4rem", height: "1.4rem" }}
                            />
                            Loading reportsâ€¦
                        </div>
                    )}

                    {!listLoading && !selectedPeriod && (
                        <div className="rpt-empty">
                            <span className="rpt-empty-icon">ðŸ“…</span>
                            <span>Select a period above to view reports</span>
                        </div>
                    )}

                    {!listLoading && selectedPeriod && reportList.length === 0 && (
                        <div className="rpt-empty">
                            <span className="rpt-empty-icon">ðŸ“­</span>
                            <span>No reports found for the selected period</span>
                        </div>
                    )}

                    {!listLoading && reportList.length > 0 && (
                        <div className="rpt-table-wrap">
                            <table className="rpt-table">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>Scope</th>
                                        <th>Data Of</th>
                                        <th>Ideas Submitted</th>
                                        <th>Approved</th>
                                        <th>Participation</th>
                                        <th>Created By</th>
                                        <th>Created At</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {reportList.map(rep => (
                                        <tr key={rep.id}>
                                            <td className="rpt-td-muted">{rep.id}</td>
                                            <td>
                                                <span className={scopeBadgeClass(rep.scope)}>
                                                    {rep.scope}
                                                </span>
                                            </td>
                                            <td style={{ fontWeight: 600 }}>{rep.dataOf}</td>
                                            <td className="rpt-td-muted">{rep.ideasSubmitted}</td>
                                            <td className="rpt-td-muted">{rep.approvedCount}</td>
                                            <td className="rpt-td-muted">{rep.participationCount}</td>
                                            <td className="rpt-td-muted">{rep.userName}</td>
                                            <td className="rpt-td-muted">
                                                {new Date(rep.createdAt).toLocaleString("en-GB", {
                                                    day: "2-digit", month: "short", year: "numeric",
                                                    hour: "2-digit", minute: "2-digit",
                                                })}
                                            </td>
                                            <td>
                                                <button
                                                    className="rpt-btn-delete"
                                                    onClick={() => setDeleteTarget(rep.id)}
                                                >
                                                    <Trash2 size={13} />
                                                    Delete
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </div>

            {/* -- Delete confirmation ---------------------------- */}
            <ConfirmationModal
                isOpen={deleteTarget !== null}
                title="Delete Report"
                message="Are you sure you want to delete this report? This action cannot be undone."
                confirmText="Delete"
                cancelText="Cancel"
                isDangerous
                onConfirm={doDelete}
                onCancel={() => setDeleteTarget(null)}
            />

            {/* -- Generate Report Modal -------------------------- */}
            {showModal && (
                <div className="rpt-modal-overlay" onClick={closeModal}>
                    <div className="rpt-modal-card" onClick={e => e.stopPropagation()}>

                        {/* Header */}
                        <div className="rpt-modal-header">
                            <span className="rpt-modal-header-icon">
                                <FileText size={18} />
                            </span>
                            <h3 className="rpt-modal-title">Generate Report</h3>
                            <button className="rpt-modal-close" onClick={closeModal} aria-label="Close">
                                <X size={15} />
                            </button>
                        </div>

                        {/* Form */}
                        <form onSubmit={handleSubmit} noValidate>
                            <div className="rpt-modal-body">

                                {/* Scope */}
                                <div className="rpt-field">
                                    <label className="rpt-field-label">
                                        Report Scope <span className="rpt-required">*</span>
                                    </label>
                                    <select
                                        className={`rpt-field-select${formErrors.scope ? " is-error" : ""}`}
                                        value={generateReportObj.scope}
                                        onChange={onScopeChange}
                                    >
                                        <option value="">- Choose scope -</option>
                                        <option value="DEPARTMENT">Department</option>
                                        <option value="CATEGORY">Category</option>
                                        <option value="PERIOD">Period</option>
                                    </select>
                                    {formErrors.scope && (
                                        <span className="rpt-field-error">âš  {formErrors.scope}</span>
                                    )}
                                </div>

                                {/* Department picker */}
                                {generateReportObj.scope === "DEPARTMENT" && (
                                    <div className="rpt-field rpt-field-animated">
                                        <label className="rpt-field-label">
                                            Department <span className="rpt-required">*</span>
                                        </label>
                                        <select
                                            className={`rpt-field-select${formErrors.scopeId ? " is-error" : ""}`}
                                            value={generateReportObj.scopeId || ""}
                                            onChange={handleDeptChange}
                                        >
                                            <option value="">- Select department -</option>
                                            {departments.map(d => (
                                                <option key={d.deptId} value={d.deptId}>{d.deptName}</option>
                                            ))}
                                        </select>
                                        {formErrors.scopeId && (
                                            <span className="rpt-field-error">âš  {formErrors.scopeId}</span>
                                        )}
                                    </div>
                                )}

                                {/* Category picker */}
                                {generateReportObj.scope === "CATEGORY" && (
                                    <div className="rpt-field rpt-field-animated">
                                        <label className="rpt-field-label">
                                            Category <span className="rpt-required">*</span>
                                        </label>
                                        <select
                                            className={`rpt-field-select${formErrors.scopeId ? " is-error" : ""}`}
                                            value={generateReportObj.scopeId || ""}
                                            onChange={handleCategoryChange}
                                        >
                                            <option value="">- Select category -</option>
                                            {categories.map(c => (
                                                <option key={c.categoryId} value={c.categoryId}>{c.name}</option>
                                            ))}
                                        </select>
                                        {formErrors.scopeId && (
                                            <span className="rpt-field-error">âš  {formErrors.scopeId}</span>
                                        )}
                                    </div>
                                )}

                                {/* Period picker */}
                                {generateReportObj.scope === "PERIOD" && (
                                    <div className="rpt-field rpt-field-animated">
                                        <label className="rpt-field-label">
                                            Month &amp; Year <span className="rpt-required">*</span>
                                        </label>
                                        <input
                                            ref={modalPeriodRef}
                                            type="month"
                                            className={`rpt-field-input${formErrors.period ? " is-error" : ""}`}
                                            onChange={handlePeriodChange}
                                        />
                                        {formErrors.period && (
                                            <span className="rpt-field-error">âš  {formErrors.period}</span>
                                        )}
                                    </div>
                                )}

                            </div>

                            {/* Footer */}
                            <div className="rpt-modal-footer">
                                <button type="button" className="rpt-modal-cancel" onClick={closeModal}>
                                    Cancel
                                </button>
                                <button type="submit" className="rpt-modal-submit" disabled={submitting}>
                                    {submitting ? (
                                        <>
                                            <span
                                                className="spinner-border spinner-border-sm"
                                                role="status"
                                                style={{ width: "0.85rem", height: "0.85rem" }}
                                            />
                                            Generatingâ€¦
                                        </>
                                    ) : (
                                        <>
                                            <FileText size={14} />
                                            Generate Report
                                        </>
                                    )}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ReportCreationAndDisplay;

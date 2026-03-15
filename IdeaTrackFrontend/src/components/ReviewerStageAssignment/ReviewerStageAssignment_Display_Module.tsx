import { useEffect, useState } from "react";
import { assignedReviewerDetails, removeReviewerFromStage, type IAssignedReviewer } from "../../utils/reviewerAssignmentApi";
import ConfirmationModal from "../ConfirmationModal/ConfirmationModal";
import { useShowToast } from "../../hooks/useShowToast";
import { ClipboardList, Trash2, Users } from "lucide-react";
import "./ReviewerStageAssignment.css";

const ReviewerStageAssignment_Display_Module = () => {
    const toast = useShowToast();
    const [assignedReviewer, setAssignedReviewer] = useState<IAssignedReviewer[]>([]);
    const [loading, setLoading] = useState(false);
    const [confirmTarget, setConfirmTarget] = useState<{ reviewerId: number; categoryId: number; stageNo: number } | null>(null);

    // Fetch assigned reviewers
    const fetchData = () => {
        setLoading(true);
        assignedReviewerDetails()
            .then(res => setAssignedReviewer(res.data))
            .catch(err => console.error(err))
            .finally(() => setLoading(false));
    };

    // Auto-load on mount
    useEffect(() => { fetchData(); }, []);

    // Trigger delete confirmation
    const deleteData = (reviewerId: number, categoryId: number, stageNo: number) => {
        setConfirmTarget({ reviewerId, categoryId, stageNo });
    };

    // Perform the delete
    const doDelete = () => {
        if (!confirmTarget) return;
        const { reviewerId, categoryId, stageNo } = confirmTarget;
        setConfirmTarget(null);
        removeReviewerFromStage(reviewerId, categoryId, stageNo)
            .then(() => {
                toast.success("Reviewer assignment removed successfully.");
                fetchData();
            })
            .catch(err => {
                console.error(err);
                toast.error("Failed to remove reviewer assignment.");
            });
    };

    return (
        <>
            <div className="rsa-card">
                {/* Card header */}
                <div className="rsa-card-header">
                    <h2 className="rsa-card-title">
                        <span className="rsa-card-title-icon">
                            <ClipboardList size={17} />
                        </span>
                        Assigned Reviewers
                    </h2>
                </div>

                <div style={{ padding: loading || assignedReviewer.length === 0 ? "24px" : 0 }}>
                    {loading && (
                        <div className="rsa-empty">
                            <span
                                className="spinner-border spinner-border-sm text-primary"
                                role="status"
                                style={{ width: "1.5rem", height: "1.5rem" }}
                            />
                            <span style={{ color: "var(--rsa-muted)" }}>Loading assignments…</span>
                        </div>
                    )}

                    {!loading && assignedReviewer.length === 0 && (
                        <div className="rsa-empty">
                            <span className="rsa-empty-icon"><Users size={28} /></span>
                            <span>No reviewer assignments found</span>
                        </div>
                    )}

                    {!loading && assignedReviewer.length > 0 && (
                        <div className="rsa-table-wrap">
                            <table className="rsa-table">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>Reviewer Name</th>
                                        <th>Category</th>
                                        <th>Stage</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {assignedReviewer.map(reviewer => (
                                        <tr key={`${reviewer.reviewerId}-${reviewer.categoryId}-${reviewer.stageNo}`}>
                                            <td className="rsa-td-muted">{reviewer.reviewerId}</td>
                                            <td style={{ fontWeight: 600 }}>{reviewer.name}</td>
                                            <td>
                                                <span className="rsa-badge">{reviewer.categoryName}</span>
                                            </td>
                                            <td>
                                                <span className="rsa-badge-stage">Stage {reviewer.stageNo}</span>
                                            </td>
                                            <td>
                                                <button
                                                    className="rsa-btn-delete"
                                                    onClick={() => deleteData(reviewer.reviewerId, reviewer.categoryId, reviewer.stageNo)}
                                                >
                                                    <Trash2 size={13} />
                                                    Remove
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

            <ConfirmationModal
                isOpen={confirmTarget !== null}
                title="Remove Reviewer Assignment"
                message="Are you sure you want to remove this reviewer assignment? This action cannot be undone."
                confirmText="Remove"
                cancelText="Cancel"
                isDangerous
                onConfirm={doDelete}
                onCancel={() => setConfirmTarget(null)}
            />
        </>
    );
};

export default ReviewerStageAssignment_Display_Module;
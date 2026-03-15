import { useState } from "react";
import ReviewerStageAssignment_Creation_Module from "../../components/ReviewerStageAssignment/ReviewerStageAssignment_Creation_Module";
import ReviewerStageAssignment_Display_Module from "../../components/ReviewerStageAssignment/ReviewerStageAssignment_Display_Module";
import "../../components/ReviewerStageAssignment/ReviewerStageAssignment.css";
import { UserCheck, ClipboardList } from "lucide-react";

type InnerTab = "assign" | "view";

const ReviewerStageAssignment = () => {
    const [innerTab, setInnerTab] = useState<InnerTab>("assign");

    return (
        <div className="rsa-page" style={{ paddingTop: 20 }}>
            {/* Inner tab nav */}
            <div className="rsa-inner-tabs">
                <button
                    className={`rsa-inner-tab ${innerTab === "assign" ? "active" : ""}`}
                    onClick={() => setInnerTab("assign")}
                >
                    <UserCheck size={15} />
                    Assign Reviewer
                </button>
                <button
                    className={`rsa-inner-tab ${innerTab === "view" ? "active" : ""}`}
                    onClick={() => setInnerTab("view")}
                >
                    <ClipboardList size={15} />
                    View Assignments
                </button>
            </div>

            {/* Active panel */}
            {innerTab === "assign" ? (
                <ReviewerStageAssignment_Creation_Module />
            ) : (
                <ReviewerStageAssignment_Display_Module />
            )}
        </div>
    );
};

export default ReviewerStageAssignment;

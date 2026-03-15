import { assignReviewerToStage, getAvailableReviewersList, getCategoriesAndStageCountByCategory, type IAvailableReviewer, type ICategory, type IReviewerAssignment, type IViewDept } from "../../utils/reviewerAssignmentApi";
import { useEffect, useRef, useState } from "react";
import "./ReviewerStageAssignment.css";
import restApi from "../../utils/restApi";
import { useShowToast } from "../../hooks/useShowToast";
import { UserCheck, Users, Inbox as InboxIcon } from "lucide-react";

// getDepartments for IViewDept list
const getAllDept = () => restApi.get('/api/profile/departmentID');

const ReviewerStageAssignment_Creation_Module = () => {
    const toast = useShowToast();

    const [availableReviewer, setAvailableReviewer] = useState<IAvailableReviewer[]>([]);
    const [categoriesAndStageCount, setCategoriesAndStageCount] = useState<ICategory[]>([]);
    const [departments, setDepartments] = useState<IViewDept[]>([]);
    const [stageCount, setStageCount] = useState(0);

    const [selectedDept, setSelectedDept] = useState(0);
    const [selectedCategory, setSelectedCategory] = useState(0);
    const [selectedStage, setSelectedStage] = useState(0);
    const [selectedReviewer, setSelectedReviewer] = useState(0);

    const deptRef = useRef<HTMLSelectElement>(null);
    const categoryRef = useRef<HTMLSelectElement>(null);
    const stageRef = useRef<HTMLSelectElement>(null);
    const reviewerRef = useRef<HTMLSelectElement>(null);

    // Fetch departments on mount
    useEffect(() => {
        const fetchDepartments = async () => {
            try {
                const response = await getAllDept();
                setDepartments(response.data);
            } catch (err) {
                console.error("Failed to fetch departments:", err);
            }
        };
        fetchDepartments();
    }, []);

    // Executed when the form is submitted
    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();

        if (!selectedCategory || !selectedStage || !selectedReviewer) {
            toast.warning("Please fill in all fields before assigning.");
            return;
        }

        const assignmentData: IReviewerAssignment = {
            reviewerId: selectedReviewer,
            categoryId: selectedCategory,
            stageNo: selectedStage,
        };

        assignReviewerToStage(assignmentData)
            .then(() => {
                toast.success("Reviewer assigned successfully.");
                // Reset dropdowns
                if (deptRef.current) deptRef.current.value = "";
                if (categoryRef.current) categoryRef.current.value = "";
                if (stageRef.current) stageRef.current.value = "";
                if (reviewerRef.current) reviewerRef.current.value = "";

                setSelectedDept(0);
                setSelectedCategory(0);
                setSelectedStage(0);
                setSelectedReviewer(0);
                setStageCount(0);
                setAvailableReviewer([]);
            })
            .catch(err => console.error(err));
    };

    // Executed when the department is changed
    const handleDeptChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        const deptId = Number(e.target.value);
        setSelectedDept(deptId);
        setSelectedCategory(0);
        setSelectedStage(0);
        setSelectedReviewer(0);
        setStageCount(0);
        if (categoryRef.current) categoryRef.current.value = "";
        if (stageRef.current) stageRef.current.value = "";
        if (reviewerRef.current) reviewerRef.current.value = "";

        getAvailableReviewersList(deptId)
            .then(res => setAvailableReviewer(res.data))
            .catch(err => console.error(err));

        getCategoriesAndStageCountByCategory(deptId)
            .then(res => setCategoriesAndStageCount(res.data))
            .catch(err => console.error(err));
    };

    // Executed when the category is changed
    const handleCategoryChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        const catId = Number(e.target.value);
        setSelectedCategory(catId);
        setSelectedStage(0);
        setSelectedReviewer(0);
        if (stageRef.current) stageRef.current.value = "";
        if (reviewerRef.current) reviewerRef.current.value = "";

        const selectCatObj = categoriesAndStageCount.find(v => v.categoryId === catId);
        if (selectCatObj) setStageCount(selectCatObj.stageCount);
    };

    const handleStageChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        setSelectedStage(Number(e.target.value));
        setSelectedReviewer(0);
        if (reviewerRef.current) reviewerRef.current.value = "";
    };

    const handleReviewerChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        setSelectedReviewer(Number(e.target.value));
    };

    return (
        <div className="rsa-card">
            {/* Card header */}
            <div className="rsa-card-header">
                <h2 className="rsa-card-title">
                    <span className="rsa-card-title-icon">
                        <UserCheck size={17} />
                    </span>
                    Assign Reviewer to Category &amp; Stage
                </h2>
            </div>

            <div className="rsa-card-body">
                <div className="rsa-two-col">

                    {/* ── Form column ── */}
                    <form onSubmit={handleSubmit}>

                        <div className="rsa-form-group">
                            <label className="rsa-label">Department</label>
                            <select
                                ref={deptRef}
                                className="rsa-select"
                                onChange={handleDeptChange}
                            >
                                <option value="">— Choose Department —</option>
                                {departments.map(dept => (
                                    <option key={dept.deptId} value={dept.deptId}>
                                        {dept.deptName}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className="rsa-form-group">
                            <label className="rsa-label">Category</label>
                            <select
                                ref={categoryRef}
                                className="rsa-select"
                                onChange={handleCategoryChange}
                                disabled={selectedDept === 0}
                            >
                                <option value="">— Choose Category —</option>
                                {categoriesAndStageCount.map(cat => (
                                    <option key={cat.categoryId} value={cat.categoryId}>
                                        {cat.categoryName}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className="rsa-form-group">
                            <label className="rsa-label">Stage</label>
                            <select
                                ref={stageRef}
                                className="rsa-select"
                                onChange={handleStageChange}
                                disabled={selectedCategory === 0}
                            >
                                <option value="">— Choose Stage —</option>
                                {stageCount > 0 &&
                                    Array.from({ length: stageCount }, (_, i) => i + 1).map(num => (
                                        <option key={num} value={num}>Stage {num}</option>
                                    ))}
                            </select>
                        </div>

                        <div className="rsa-form-group">
                            <label className="rsa-label">Reviewer</label>
                            <select
                                ref={reviewerRef}
                                className="rsa-select"
                                onChange={handleReviewerChange}
                                disabled={selectedStage === 0}
                            >
                                <option value="">— Choose Reviewer —</option>
                                {availableReviewer.map(rev => (
                                    <option key={rev.userId} value={rev.userId}>
                                        {rev.name}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <button className="rsa-btn-primary" type="submit">
                            <UserCheck size={16} />
                            Assign Reviewer
                        </button>
                    </form>

                    {/* ── Available Reviewers panel ── */}
                    <div className="rsa-reviewers-panel">
                        <div className="rsa-reviewers-panel-header">
                            <Users size={14} />
                            Available Reviewers
                        </div>

                        {availableReviewer.length > 0 ? (
                            <div className="rsa-table-wrap">
                                <table className="rsa-table">
                                    <thead>
                                        <tr>
                                            <th>ID</th>
                                            <th>Name</th>
                                            <th>Department</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {availableReviewer.map(reviewer => (
                                            <tr key={reviewer.userId}>
                                                <td className="rsa-td-muted">{reviewer.userId}</td>
                                                <td style={{ fontWeight: 600 }}>{reviewer.name}</td>
                                                <td className="rsa-td-muted">{reviewer.deptName}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <div className="rsa-empty">
                                <span className="rsa-empty-icon"><InboxIcon size={28} /></span>
                                <span>No available reviewers</span>
                                {selectedDept === 0 && (
                                    <span style={{ fontSize: "0.78rem", opacity: 0.7 }}>Select a department to see reviewers</span>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ReviewerStageAssignment_Creation_Module;

import { useState, useEffect, useCallback } from "react";
import { useForm } from "react-hook-form";
import {
  Plus,
  FolderOpen,
  CheckCircle,
  MoreVertical,
  Pencil,
  Trash2,
  Save,
  ArrowLeft,
} from "lucide-react";
import {
  fetchAllCategories,
  createCategory,
  updateCategory,
  deleteCategory,
} from "../../utils/categoryApi";
import { getAllDept } from "../../utils/analyticsApi";
import { fetchMyProfile } from "../../utils/profileApi";
import type {
  CategoryResponse,
  CategoryCreateRequest,
  DepartmentMiniDTO,
} from "../../utils/types";
import ConfirmationModal from "../ConfirmationModal/ConfirmationModal";
import { useShowToast } from "../../hooks/useShowToast";
import "./CategoryManagement.css";

/* ── Form field types ────────────────────────────────────────── */
interface CategoryFormValues {
  name: string;
  departmentId: string; // select returns string
  reviewerCountPerStage: number;
  stageCount: number;
}

const CategoryManagement = () => {
  // ── state ──────────────────────────────────────────────────
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [departments, setDepartments] = useState<DepartmentMiniDTO[]>([]);
  const [adminUserId, setAdminUserId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [view, setView] = useState<"list" | "form">("list");
  const [editId, setEditId] = useState<number | null>(null);
  const [openMenu, setOpenMenu] = useState<number | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);
  const toast = useShowToast();

  // ── react-hook-form ────────────────────────────────────────
  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { errors },
  } = useForm<CategoryFormValues>();

  // ── load categories & extract departments on mount ──────────
  const loadCategories = useCallback(async () => {
    setLoading(true);
    try {
      const data = await fetchAllCategories();
      setCategories(data);
    } catch (err) {
      console.error("Failed to load categories", err);
      toast.error("Failed to load categories");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadCategories();
    fetchMyProfile()
      .then((p) => setAdminUserId(p.userId))
      .catch(() => console.error("Failed to load profile"));
    getAllDept()
      .then((res) => setDepartments(res.data))
      .catch(() => console.error("Failed to load departments"));
  }, [loadCategories]);

  // close action menu on outside click
  useEffect(() => {
    const close = () => setOpenMenu(null);
    window.addEventListener("click", close);
    return () => window.removeEventListener("click", close);
  }, []);

  // ── form actions ───────────────────────────────────────────
  const openAddForm = () => {
    setEditId(null);
    reset({ name: "", departmentId: "", reviewerCountPerStage: 2, stageCount: 2 });
    setView("form");
  };

  const openEditForm = (cat: CategoryResponse) => {
    setEditId(cat.categoryId);
    setValue("name", cat.name);
    setValue("departmentId", String(cat.department.deptId));
    setValue("reviewerCountPerStage", cat.reviewerCountPerStage);
    setValue("stageCount", cat.stageCount);
    setView("form");
    setOpenMenu(null);
  };

  const onSubmit = async (data: CategoryFormValues) => {
    try {
      if (editId) {
        await updateCategory(editId, {
          name: data.name,
          departmentId: Number(data.departmentId),
          reviewerCountPerStage: data.reviewerCountPerStage,
          stageCount: data.stageCount,
        });
        toast.success("Category updated successfully");
      } else {
        const payload: CategoryCreateRequest = {
          name: data.name,
          departmentId: Number(data.departmentId),
          createdByAdminId: adminUserId ?? 0,
          reviewerCountPerStage: data.reviewerCountPerStage,
          stageCount: data.stageCount,
        };
        await createCategory(payload);
        toast.success("Category created successfully");
      }
      setView("list");
      loadCategories();
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "Operation failed";
      toast.error(`${msg}`);
    }
  };

  const handleDelete = async (id: number) => {
    setConfirmDeleteId(id);
    setOpenMenu(null);
  };

  const doDeleteCategory = async () => {
    if (confirmDeleteId === null) return;
    const id = confirmDeleteId;
    setConfirmDeleteId(null);
    try {
      await deleteCategory(id);
      toast.success("Category deleted");
      loadCategories();
    } catch {
      toast.error("Failed to delete category");
    }
  };

  // ── render ─────────────────────────────────────────────────
  return (
    <div className="cat-page">
      {/* ── LIST VIEW ─────────────────────────────────────────── */}
      {view === "list" && (
        <div className="card p-4">
          {/* header */}
          <div className="d-flex justify-content-between align-items-center mb-4">
            <div>
              <h2 className="mb-1">Category Management</h2>
              <p className="text-muted mb-0" style={{ fontSize: 14 }}>
                Organize ideas into categories with custom review stages
              </p>
            </div>
            <button className="btn btn-primary d-flex align-items-center gap-1" onClick={openAddForm}>
              <Plus size={16} /> Add Category
            </button>
          </div>

          {/* stats */}
          <div className="cat-stats">
            <div className="cat-stat-card">
              <div className="cat-stat-icon purple"><FolderOpen size={24} /></div>
              <div className="cat-stat-info">
                <h4>{categories.length}</h4>
                <p>Total Categories</p>
              </div>
            </div>
            <div className="cat-stat-card">
              <div className="cat-stat-icon green"><CheckCircle size={24} /></div>
              <div className="cat-stat-info">
                <h4>{categories.filter((c) => !c.deleted).length}</h4>
                <p>Active</p>
              </div>
            </div>
          </div>

          {/* table */}
          {loading ? (
            <div className="text-center py-5">
              <div className="spinner-border text-primary" role="status" />
            </div>
          ) : categories.length === 0 ? (
            <div className="cat-empty">
              <p className="mb-0">No categories yet. Click "Add Category" to create one.</p>
            </div>
          ) : (
            <div className="table-responsive">
              <table className="cat-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Name</th>
                    <th>Department</th>
                    <th>Stages</th>
                    <th>Reviewers / Stage</th>
                    <th>Created By</th>
                    <th>Created</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {categories.map((cat, idx) => (
                    <tr key={cat.categoryId}>
                      <td>{idx + 1}</td>
                      <td className="fw-semibold">{cat.name}</td>
                      <td>
                        <span className="dept-pill">
                          {cat.department?.deptName ?? "—"}
                        </span>
                      </td>
                      <td>{cat.stageCount}</td>
                      <td>{cat.reviewerCountPerStage}</td>
                      <td>{cat.createdByAdmin?.name ?? "—"}</td>
                      <td style={{ fontSize: 13, color: "#6b7280" }}>
                        {cat.createdAt
                          ? new Date(cat.createdAt).toLocaleDateString()
                          : "—"}
                      </td>
                      <td>
                        <div className="cat-action-wrapper">
                          <button
                            className="cat-action-btn"
                            onClick={(e) => {
                              e.stopPropagation();
                              setOpenMenu(
                                openMenu === cat.categoryId
                                  ? null
                                  : cat.categoryId
                              );
                            }}
                          >
                            <MoreVertical size={18} />
                          </button>
                          {openMenu === cat.categoryId && (
                            <div className="cat-action-menu">
                              <button onClick={() => openEditForm(cat)}>
                                <Pencil size={14} /> Edit
                              </button>
                              <button
                                className="danger"
                                onClick={() => handleDelete(cat.categoryId)}
                              >
                                <Trash2 size={14} /> Delete
                              </button>
                            </div>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* ── FORM VIEW ─────────────────────────────────────────── */}
      {view === "form" && (
        <div className="cat-form-card">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h2 className="mb-0">
              {editId ? "Edit Category" : "Add New Category"}
            </h2>
            <button
              className="btn btn-outline-secondary d-flex align-items-center gap-1"
              onClick={() => setView("list")}
            >
              <ArrowLeft size={16} /> Back
            </button>
          </div>

          <form onSubmit={handleSubmit(onSubmit)}>
            {/* Name */}
            <div className="mb-3">
              <label className="form-label fw-bold">Category Name *</label>
              <input
                className={`form-control ${errors.name ? "is-invalid" : ""}`}
                placeholder="e.g., Artificial Intelligence & Machine Learning"
                {...register("name", {
                  required: "Category name is required",
                  maxLength: {
                    value: 255,
                    message: "Name cannot exceed 255 characters",
                  },
                })}
              />
              {errors.name && (
                <div className="field-error">{errors.name.message}</div>
              )}
            </div>

            <div className="cat-form-row mb-3">
              {/* Department */}
              <div>
                <label className="form-label fw-bold">Department *</label>
                <select
                  className={`form-select ${errors.departmentId ? "is-invalid" : ""}`}
                  {...register("departmentId", {
                    required: "Department is required",
                  })}
                >
                  <option value="">Select department</option>
                  {departments.map((d) => (
                    <option key={d.deptId} value={d.deptId}>
                      {d.deptName}
                    </option>
                  ))}
                </select>
                {errors.departmentId && (
                  <div className="field-error">
                    {errors.departmentId.message}
                  </div>
                )}
              </div>

              {/* Reviewers per stage */}
              <div>
                <label className="form-label fw-bold">
                  Reviewers Per Stage *
                </label>
                <input
                  type="number"
                  className={`form-control ${errors.reviewerCountPerStage ? "is-invalid" : ""}`}
                  {...register("reviewerCountPerStage", {
                    required: "Required",
                    min: { value: 0, message: "Minimum is 0" },
                    max: { value: 10, message: "Maximum is 10" },
                    valueAsNumber: true,
                  })}
                />
                <small className="text-muted">
                  How many reviewers evaluate each stage
                </small>
                {errors.reviewerCountPerStage && (
                  <div className="field-error">
                    {errors.reviewerCountPerStage.message}
                  </div>
                )}
              </div>
            </div>

            <div className="cat-form-row mb-3">
              {/* Stage count */}
              <div>
                <label className="form-label fw-bold">
                  Number of Review Stages *
                </label>
                <input
                  type="number"
                  className={`form-control ${errors.stageCount ? "is-invalid" : ""}`}
                  {...register("stageCount", {
                    required: "Required",
                    min: { value: 1, message: "Minimum is 1" },
                    max: { value: 5, message: "Maximum is 5" },
                    valueAsNumber: true,
                  })}
                />
                <small className="text-muted">
                  Total review stages before approval
                </small>
                {errors.stageCount && (
                  <div className="field-error">
                    {errors.stageCount.message}
                  </div>
                )}
              </div>
              <div />
            </div>

            <div className="d-flex gap-2 mt-4">
              <button type="submit" className="btn btn-primary d-flex align-items-center gap-1">
                <Save size={16} /> {editId ? "Update" : "Save"} Category
              </button>
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={() => setView("list")}
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {/* ── Confirmation Modal ──────────────────────────────────── */}
      <ConfirmationModal
        isOpen={confirmDeleteId !== null}
        title="Delete Category"
        message="Are you sure you want to delete this category? This cannot be undone."
        confirmText="Delete"
        cancelText="Cancel"
        isDangerous
        onConfirm={doDeleteCategory}
        onCancel={() => setConfirmDeleteId(null)}
      />
    </div>
  );
};

export default CategoryManagement;

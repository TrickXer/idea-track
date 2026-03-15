import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { IdeaCreateRequest } from '../../components/idea/IdeaCreateRequest';
import { 
  getIdeaDetails, 
  updateIdea, 
  submitFinalIdea,
  getCategories
} from '../../utils/ideaApi';
import { useShowToast } from '../../hooks/useShowToast';

type Category = { categoryId: number; name: string };

const EditDraftForm: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const toast = useShowToast();
  const ideaIdNum = Number(id);

  const [form, setForm] = useState<IdeaCreateRequest>({
    title: '',
    description: '',
    problemStatement: '',
    categoryId: 0,
    tag: '',
    thumbnailURL: '',
  });

  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const loadInitialData = async () => {
      try {
        setLoading(true);
        const [catRes, ideaRes] = await Promise.all([
          getCategories(),
          getIdeaDetails(ideaIdNum, 0) 
        ]);

        setCategories(catRes.data);
        const idea = ideaRes.data;
        
        setForm({
          title: idea.title || '',
          description: idea.description || '',
          problemStatement: idea.problemStatement || '',
          categoryId: idea.category?.categoryId || 0,
          tag: idea.tag || '',
          thumbnailURL: idea.thumbnailURL || '',
        });
      } catch (err) {
        toast.error("Could not load draft data.");
      } finally {
        setLoading(false);
      }
    };
    loadInitialData();
  }, [ideaIdNum]);

  const update = <K extends keyof IdeaCreateRequest>(key: K, value: IdeaCreateRequest[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  const handleAction = async (e: React.FormEvent, actionType: 'save' | 'submit') => {
    e.preventDefault();

    try {
      setSubmitting(true);
      
      if (actionType === 'save') {
        await updateIdea(ideaIdNum, form);
        toast.success('Draft updated successfully!');
      } else {
        await submitFinalIdea(ideaIdNum);
        toast.success('Idea submitted successfully!');
      }

      navigate('/EmployeeDashboard');

    } catch (err: any) {
      console.error('Action failed:', err);
      const errMsg = err?.response?.data?.message || 'Failed to process request.';
      toast.error(errMsg);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return (
    <div className="d-flex justify-content-center align-items-center vh-100">
      <div className="spinner-border text-primary" role="status"></div>
    </div>
  );

  return (
    <div className="container py-5">
      <div className="card border-0 shadow-sm rounded-4 mx-auto" style={{ maxWidth: '800px' }}>
        <div className="card-body p-4">
          <h4 className="fw-bold mb-4">Edit Your Draft</h4>

          <form noValidate>
            <div className="mb-3">
              <label className="form-label fw-bold">Title</label>
              <input
                type="text"
                className="form-control"
                value={form.title}
                onChange={(e) => update('title', e.target.value)}
              />
            </div>

            <div className="mb-3">
              <label className="form-label fw-bold">Category</label>
              <select
                className="form-select"
                value={form.categoryId}
                onChange={(e) => update('categoryId', Number(e.target.value))}
              >
                <option value={0} disabled>Select category</option>
                {categories.map((cat) => (
                  <option key={cat.categoryId} value={cat.categoryId}>{cat.name}</option>
                ))}
              </select>
            </div>

            <div className="mb-3">
              <label className="form-label fw-bold">Problem Statement</label>
              <textarea
                className="form-control"
                rows={3}
                value={form.problemStatement}
                onChange={(e) => update('problemStatement', e.target.value)}
              />
            </div>

            <div className="mb-3">
              <label className="form-label fw-bold">Description</label>
              <textarea
                className="form-control"
                rows={5}
                value={form.description}
                onChange={(e) => update('description', e.target.value)}
              />
            </div>

            <div className="mb-4">
              <label className="form-label fw-bold">Thumbnail/Link URL</label>
              <input
                type="url"
                className="form-control"
                value={form.thumbnailURL}
                onChange={(e) => update('thumbnailURL', e.target.value)}
              />
            </div>

            <div className="d-flex justify-content-end gap-2 pt-4 border-top">
              <button 
                type="button" 
                className="btn btn-outline-secondary rounded-pill px-4" 
                onClick={() => navigate('/EmployeeDashboard')}
              >
                Cancel
              </button>
              
              <button 
                type="button" 
                className="btn btn-light border rounded-pill px-4" 
                onClick={(e) => handleAction(e, 'save')}
                disabled={submitting}
              >
                {submitting ? 'Saving...' : 'Save Changes'}
              </button>

              <button 
                type="button" 
                className="btn btn-primary rounded-pill px-4" 
                onClick={(e) => handleAction(e, 'submit')}
                disabled={submitting}
              >
                Submit Final
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default EditDraftForm;
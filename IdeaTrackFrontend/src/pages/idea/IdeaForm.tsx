import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { IdeaCreateRequest } from '../../components/idea/IdeaCreateRequest';
import { saveDraft, submitFinalIdea, getCategories } from '../../utils/ideaApi';
import { useShowToast } from '../../hooks/useShowToast';
import { Lightbulb, Tag, Image, AlignLeft, HelpCircle, FolderOpen, X, Save } from 'lucide-react';

type Category = { categoryId: number; name: string };

type CreateIdeaFormProps = {
  categories?: Category[];
  onCreated?: () => void;
};

const initialForm: IdeaCreateRequest = {
  title: '',
  description: '',
  problemStatement: '',
  categoryId: 0,
  tag: '',
  thumbnailURL: '',
};

const fieldStyle: React.CSSProperties = {
  width: '100%', padding: '12px 16px', fontSize: 14,
  borderRadius: 12, border: '1.5px solid #E9EDF7',
  background: '#F8FAFF', outline: 'none', color: '#1B254B',
  transition: '0.2s ease', boxSizing: 'border-box', fontFamily: 'inherit',
};

const labelStyle: React.CSSProperties = {
  display: 'flex', alignItems: 'center', gap: 7,
  fontSize: 13, fontWeight: 700, color: '#1B254B', marginBottom: 7,
};

const sectionStyle: React.CSSProperties = {
  background: 'white', borderRadius: 16, padding: '22px 24px',
  boxShadow: '0 2px 10px rgba(67,24,255,0.06)', marginBottom: 16,
};

const CreateIdeaForm: React.FC<CreateIdeaFormProps> = ({ categories: propCategories, onCreated }) => {
  const [form, setForm] = useState<IdeaCreateRequest>(initialForm);
  const [loading, setLoading] = useState(false);
  const [validated, setValidated] = useState(false);
  const [categories, setCategories] = useState<Category[]>(propCategories ?? []);
  const toast = useShowToast();

  const navigate = useNavigate();

  useEffect(() => {
    if (propCategories && propCategories.length > 0) return;
    getCategories()
      .then((res) => setCategories(res.data))
      .catch((err) => console.error('Failed to load categories', err));
  }, [propCategories]);

  const update = <K extends keyof IdeaCreateRequest>(key: K, value: IdeaCreateRequest[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  const handleAction = async (e: React.FormEvent, isDraft: boolean) => {
    e.preventDefault();

    if (!isDraft) {
      setValidated(true);
      if (!form.title.trim() || form.categoryId === 0) {
        toast.warning('Please fill in all required fields.');
        return;
      }
    }

    const payload: IdeaCreateRequest = {
      ...form,
      title: form.title.trim(),
      description: form.description?.trim() ?? '',
      problemStatement: form.problemStatement?.trim() ?? '',
      tag: form.tag?.trim() ?? '',
      thumbnailURL: form.thumbnailURL?.trim() ?? '',
    };

    try {
      setLoading(true);
      const response = await saveDraft(payload);
      if (!isDraft) {
        const ideaId = response.data.ideaId;
        await submitFinalIdea(ideaId);
        toast.success('Idea submitted successfully!');
      } else {
        toast.success('Draft saved successfully!');
      }
      setForm(initialForm);
      setValidated(false);
      onCreated?.();
      
      if (isDraft) {
        navigate('/EmployeeDashBoard');
      }
    } catch (err: any) {
      console.error('Action failed:', err);
      const backendMsg =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        'Failed to process idea. Please try again.';
      toast.error(backendMsg);
    } finally {
      setLoading(false);
    }
  };

  const inputFocus = (e: React.FocusEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    e.target.style.borderColor = '#4318FF';
    e.target.style.background = '#fff';
    e.target.style.boxShadow = '0 0 0 3px rgba(67,24,255,0.1)';
  };
  const inputBlur = (e: React.FocusEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    e.target.style.borderColor = '#E9EDF7';
    e.target.style.background = '#F8FAFF';
    e.target.style.boxShadow = 'none';
  };

  return (
    <div style={{ padding: '0 4px', maxWidth: 760, margin: '0 auto' }}>

      {/* Page Header */}
      <div style={{ marginBottom: 24 }}>
        <h4 style={{ fontWeight: 800, color: '#1B254B', margin: 0, marginBottom: 4, fontSize: 22 }}>
          <Lightbulb size={20} style={{ marginRight: 8, color: '#4318FF' }} />Create a New Idea
        </h4>
        <p style={{ color: '#A3AED0', fontSize: 14, margin: 0 }}>
          Share your idea with the team. Fill in the details below.
        </p>
      </div>

      <form noValidate>

        {/* Core fields */}
        <div style={sectionStyle}>
          <div style={{ fontSize: 11, fontWeight: 700, color: '#A3AED0', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 18 }}>
            Basic Info
          </div>

          {/* Title */}
          <div style={{ marginBottom: 16 }}>
            <label style={labelStyle}>
              <Lightbulb size={14} color="#4318FF" />
              Title <span style={{ color: '#EE5D50' }}>*</span>
            </label>
            <input
              type="text"
              style={{
                ...fieldStyle,
                borderColor: validated && !form.title.trim() ? '#EE5D50' : '#E9EDF7',
              }}
              placeholder="Give your idea a catchy name…"
              value={form.title}
              onChange={(e) => update('title', e.target.value)}
              onFocus={inputFocus}
              onBlur={inputBlur}
              required
            />
            {validated && !form.title.trim() && (
              <span style={{ fontSize: 11, color: '#EE5D50', marginTop: 4, display: 'block' }}>A title is required.</span>
            )}
          </div>

          {/* Category */}
          <div>
            <label style={labelStyle}>
              <FolderOpen size={14} color="#4318FF" />
              Category <span style={{ color: '#EE5D50' }}>*</span>
            </label>
            <select
              style={{
                ...fieldStyle,
                borderColor: validated && form.categoryId === 0 ? '#EE5D50' : '#E9EDF7',
                cursor: 'pointer',
              }}
              value={form.categoryId}
              onChange={(e) => update('categoryId', Number(e.target.value))}
              onFocus={inputFocus}
              onBlur={inputBlur}
              required
            >
              <option value={0} disabled>— Select a category —</option>
              {categories.map((cat) => (
                <option key={cat.categoryId} value={cat.categoryId}>{cat.name}</option>
              ))}
            </select>
            {validated && form.categoryId === 0 && (
              <span style={{ fontSize: 11, color: '#EE5D50', marginTop: 4, display: 'block' }}>Please select a category.</span>
            )}
          </div>
        </div>

        {/* Description & Problem */}
        <div style={sectionStyle}>
          <div style={{ fontSize: 11, fontWeight: 700, color: '#A3AED0', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 18 }}>
            Details
          </div>

          {/* Description */}
          <div style={{ marginBottom: 16 }}>
            <label style={labelStyle}>
              <AlignLeft size={14} color="#4318FF" />
              Description
            </label>
            <textarea
              style={{ ...fieldStyle, resize: 'vertical', minHeight: 90 }}
              rows={3}
              placeholder="Describe your idea in detail…"
              value={form.description}
              onChange={(e) => update('description', e.target.value)}
              onFocus={inputFocus}
              onBlur={inputBlur}
            />
            <div style={{ fontSize: 11, color: '#A3AED0', textAlign: 'right', marginTop: 3 }}>
              {form.description?.length ?? 0} chars
            </div>
          </div>

          {/* Problem Statement */}
          <div>
            <label style={labelStyle}>
              <HelpCircle size={14} color="#4318FF" />
              Problem Statement
            </label>
            <textarea
              style={{ ...fieldStyle, resize: 'vertical', minHeight: 90 }}
              rows={3}
              placeholder="What problem does this idea solve?"
              value={form.problemStatement}
              onChange={(e) => update('problemStatement', e.target.value)}
              onFocus={inputFocus}
              onBlur={inputBlur}
            />
            <div style={{ fontSize: 11, color: '#A3AED0', textAlign: 'right', marginTop: 3 }}>
              {form.problemStatement?.length ?? 0} chars
            </div>
          </div>
        </div>

        {/* Tags & Thumbnail */}
        <div style={sectionStyle}>
          <div style={{ fontSize: 11, fontWeight: 700, color: '#A3AED0', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 18 }}>
            Extra
          </div>

          {/* Tags */}
          <div style={{ marginBottom: 16 }}>
            <label style={labelStyle}>
              <Tag size={14} color="#4318FF" />
              Tags
              <span style={{ fontSize: 11, fontWeight: 400, color: '#A3AED0' }}>(comma-separated)</span>
            </label>
            <input
              type="text"
              style={fieldStyle}
              placeholder="AI, Automation, Fintech"
              value={form.tag}
              onChange={(e) => update('tag', e.target.value)}
              onFocus={inputFocus}
              onBlur={inputBlur}
            />
            {form.tag && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 8 }}>
                {form.tag.split(',').map(t => t.trim()).filter(Boolean).map((t, i) => (
                  <span key={i} style={{
                    background: '#EFF3FF', color: '#4318FF',
                    borderRadius: 20, padding: '3px 10px', fontSize: 11, fontWeight: 600,
                  }}>#{t}</span>
                ))}
              </div>
            )}
          </div>

          {/* Thumbnail URL */}
          <div>
            <label style={labelStyle}>
              <Image size={14} color="#4318FF" />
              Thumbnail URL
            </label>
            <input
              type="url"
              style={fieldStyle}
              value={form.thumbnailURL}
              onChange={(e) => update('thumbnailURL', e.target.value)}
              onFocus={inputFocus}
              onBlur={inputBlur}
              placeholder="https://example.com/image.jpg"
            />
          </div>
        </div>

        {/* Action Buttons */}
        <div style={{
          display: 'flex', justifyContent: 'flex-end', gap: 12,
          paddingTop: 4, flexWrap: 'wrap',
        }}>
          <button
            type="button"
            onClick={() => navigate('/EmployeeDashBoard')}
            disabled={loading}
            style={{
              background: 'white', color: '#A3AED0', border: '1.5px solid #E9EDF7',
              borderRadius: 30, padding: '11px 24px', fontWeight: 600, fontSize: 14,
              cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 7,
            }}
          >
            <X size={15} /> Cancel
          </button>

          <button
            type="button"
            onClick={(e) => handleAction(e, true)}
            disabled={loading}
            style={{
              background: '#F4F7FE', color: '#4318FF', border: 'none',
              borderRadius: 30, padding: '11px 24px', fontWeight: 700, fontSize: 14,
              cursor: loading ? 'not-allowed' : 'pointer',
              display: 'flex', alignItems: 'center', gap: 7,
              opacity: loading ? 0.6 : 1,
            }}
          >
            <Save size={15} /> {loading ? 'Saving…' : 'Save Draft'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default CreateIdeaForm;
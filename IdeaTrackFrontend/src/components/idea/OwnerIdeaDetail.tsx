import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import type { IIdea } from './IIdea';
import { getIdeaDetails, getComments, deleteIdea } from '../../utils/ideaApi';
import ConfirmationModal from '../ConfirmationModal/ConfirmationModal';
import { useShowToast } from '../../hooks/useShowToast';
import { Pencil, Trash2, Link, Search } from 'lucide-react';
 
interface IComment {
  displayName: string;
  commentText: string;
  createdAt?: string;
}
 
const OwnerIdeaDetail = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const toast = useShowToast();
 
  const [idea, setIdea] = useState<IIdea | null>(null);
  const [comments, setComments] = useState<IComment[]>([]);
  const [loading, setLoading] = useState(true);
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);
 
  const doDeleteIdea = async () => {
    setConfirmDeleteOpen(false);
    try {
      await deleteIdea(idea?.ideaId!);
      toast.success('Draft deleted successfully');
      navigate('/dashboard');
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || 'Failed to delete draft';
      toast.error(msg);
    }
  };
 
  useEffect(() => {
    const fetchData = async () => {
      try {
        if (id) {
          const ideaIdNum = Number(id);
          const userProfile = JSON.parse(localStorage.getItem('user-profile') || '{}');
          const currentUserId = userProfile.userId;
 
          const [ideaRes, commentsRes] = await Promise.all([
            getIdeaDetails(ideaIdNum, currentUserId),
            getComments(ideaIdNum)
          ]);
         
          setIdea(ideaRes.data);
          setComments(Array.isArray(commentsRes.data) ? commentsRes.data : []);
        }
      } catch (err) {
        console.error("Failed to fetch details or comments", err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id]);
 
  if (loading) return <div className="container mt-5 text-center"><div className="spinner-border text-primary"></div></div>;
  if (!idea) return <div className="container mt-5">Idea not found.</div>;
 
  return (
    <div className="container-fluid my-4 px-lg-5">
      <button className="btn btn-outline-secondary btn-sm mb-4 rounded-pill px-3" onClick={() => navigate(-1)}>
        &larr; Back to Dashboard
      </button>
 
      <div className="row g-4">
        {/* LEFT COLUMN: CONTENT */}
        <div className="col-lg-8">
          <div className="card border-0 shadow-sm rounded-4 p-4 mb-4">
            <div className="d-flex justify-content-between align-items-start mb-3">
              <span className="badge bg-primary rounded-pill px-3 py-2">
                {idea.category?.name}
              </span>
              <div className="text-end">
                {/* EDIT DRAFT BUTTON - Shown only if status is DRAFT */}
                {idea.ideaStatus?.toUpperCase() === 'DRAFT' && (
                  <>
                    <button
                      className="btn btn-warning btn-sm fw-bold rounded-pill px-3 me-2" style={{fontSize: '12px'}}
                      onClick={() => navigate(`/edit-idea/${idea.ideaId}`)}
                    >
                      <Pencil size={13} className="me-1" /> Edit Draft
                    </button>
                    <button
                      className="btn btn-danger btn-sm fw-bold rounded-pill px-3 me-2" style={{fontSize: '12px'}}
                      onClick={() => setConfirmDeleteOpen(true)}
                    >
                      <Trash2 size={13} className="me-1" /> Delete Draft
                    </button>
                  </>
                )}

                {/* EDIT REFINE BUTTON - Shown only if status is REFINE */}
                {idea.ideaStatus?.toUpperCase() === 'REFINE' && (
                  <button
                    className="btn btn-warning btn-sm fw-bold rounded-pill px-3 me-2" style={{fontSize: '12px'}}
                    onClick={() => navigate(`/edit-idea/${idea.ideaId}`)}
                  >
                    <Pencil size={13} className="me-1" /> Edit & Resubmit
                  </button>
                )}
                <small className="text-muted d-block mt-1">
                  Created: {idea.createdAt ? new Date(idea.createdAt).toLocaleDateString() : 'N/A'}
                </small>
              </div>
            </div>
           
            <h1 className="fw-bold display-6 mb-2">{idea.title}</h1>
            <p className="text-muted mb-3">By {idea.author?.displayName}</p>
 
            {idea.thumbnailURL && (
              <div className="mb-4">
                <a href={idea.thumbnailURL} target="_blank" rel="noopener noreferrer" className="btn btn-sm btn-outline-primary rounded-pill text-decoration-none">
                  <Link size={13} className="me-1" /> View Attached Link
                </a>
              </div>
            )}
           
            <hr className="my-4" />
           
            <div className="idea-sections">
              <section className="mb-4">
                <h5 className="fw-bold text-dark">Problem Statement</h5>
                <p className="text-secondary">{idea.problemStatement}</p>
              </section>
 
              <section className="mb-4">
                <h5 className="fw-bold text-dark">Description</h5>
                <p className="text-secondary" style={{ whiteSpace: 'pre-line' }}>{idea.description}</p>
              </section>
            </div>
          </div>
 
          {/* COMMENTS SECTION */}
          <div className="card border-0 shadow-sm rounded-4 p-4">
            <h5 className="fw-bold mb-4">Discussion ({comments.length})</h5>
           
            {comments.length > 0 ? (
              <div className="list-group list-group-flush">
                {comments.map((comment, index) => (
                  <div key={index} className="list-group-item px-0 border-0 mb-3">
                    <div className="d-flex gap-3">
                      <div className="bg-primary bg-opacity-10 text-primary rounded-circle d-flex align-items-center justify-content-center fw-bold" style={{width:'40px', height:'40px', minWidth: '40px'}}>
                        {(comment.displayName || "U").charAt(0).toUpperCase()}
                      </div>
                      <div className="w-100">
                        <div className="d-flex justify-content-between">
                          <p className="mb-1 fw-bold">{comment.displayName || "Anonymous"}</p>
                          <small className="text-muted">
                            {comment.createdAt ? new Date(comment.createdAt).toLocaleDateString() : ''}
                          </small>
                        </div>
                        <p className="text-secondary mb-0 p-2 bg-light rounded-3">
                          {comment.commentText}
                        </p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-4 text-muted">No comments yet.</div>
            )}
          </div>
        </div>
 
        {/* RIGHT COLUMN: ACTIONS & FEEDBACK */}
        <div className="col-lg-4">
          <div className="" style={{ }}>
 
            {/* Status + Hierarchy Button Card */}
            <div className="card border-0 shadow-sm rounded-4 p-4 mb-4">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <div>
                  <p className="text-uppercase small fw-bold text-muted mb-1">Current Status</p>
                  <span className={`badge rounded-pill fs-6 px-3 py-2 ${
                    idea.ideaStatus?.toUpperCase() === 'ACCEPTED' ? 'bg-success' :
                    idea.ideaStatus?.toUpperCase() === 'REJECTED' ? 'bg-danger' :
                    idea.ideaStatus?.toUpperCase() === 'DRAFT'    ? 'bg-secondary' :
                    'bg-warning text-dark'
                  }`}>
                    {idea.ideaStatus}
                  </span>
                </div>  
              </div>
 
              {/* Hierarchy button — only for ideas that have entered review */}
              {idea.ideaStatus?.toUpperCase() !== 'DRAFT' && (
                <button
                  className="btn btn-outline-primary w-100 rounded-pill fw-bold"
                  onClick={() => navigate(`/hierarchy/idea/${idea.ideaId}`)}
                >
                  <Search size={13} className="me-1" /> View Review Hierarchy
                </button>
              )}
            </div>
 
            {/* FEEDBACK CARD */}
            <div className="card border-0 shadow-sm rounded-4 p-4 mb-4 bg-light border-start border-4 border-info">
              <h6 className="fw-bold text-uppercase small text-muted mb-3">Official Feedback</h6>
              {idea.feedback && idea.feedback.length > 0 ? (
                <div className="d-flex flex-column gap-2">
                  {idea.feedback.map((item, index) => (
                    <p key={index} className="text-dark mb-0 small border-bottom pb-2" style={{ fontStyle: 'italic' }}>
                      "{item}"
                    </p>
                  ))}
                </div>
              ) : (
                <p className="text-muted mb-0 small">No official feedback has been provided yet.</p>
              )}
            </div>
 
            {/* Engagement Card */}
            <div className="card border-0 shadow-sm rounded-4 p-4 text-center">
              <h5 className="fw-bold mb-4 text-start">Engagement</h5>
              <div className="row g-2">
                <div className="col-6">
                  <div className="p-3 bg-success bg-opacity-10 rounded-3 text-success">
                    <h2 className="fw-bold mb-0">{idea.votes?.upvotes ?? 0}</h2>
                    <small className="fw-bold">Upvote</small>
                  </div>
                </div>
                <div className="col-6">
                  <div className="p-3 bg-danger bg-opacity-10 rounded-3 text-danger">
                    <h2 className="fw-bold mb-0">{idea.votes?.downvotes ?? 0}</h2>
                    <small className="fw-bold">Downvote</small>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
 
      <ConfirmationModal
        isOpen={confirmDeleteOpen}
        title="Delete Draft"
        message="Are you sure you want to delete this draft? This action cannot be undone."
        confirmText="Delete"
        cancelText="Cancel"
        isDangerous
        onConfirm={doDeleteIdea}
        onCancel={() => setConfirmDeleteOpen(false)}
      />
    </div>
  );
};
 
export default OwnerIdeaDetail;
 
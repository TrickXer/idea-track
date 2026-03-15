import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import type { IIdea } from './IIdea'; 
import { getIdeaDetails, getComments, postComment as postComments, postVote } from '../../utils/ideaApi';
import { useAuth } from '../../utils/authContext';
import { useShowToast } from '../../hooks/useShowToast';
import { Link, ThumbsUp, ThumbsDown } from 'lucide-react';

interface IComment {
  displayName: string;
  commentText: string;
  createdAt?: string; 
}

const NonOwnerIdeaDetail = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { token } = useAuth();
  const toast = useShowToast();
  
  const [idea, setIdea] = useState<IIdea | null>(null);
  const [comments, setComments] = useState<IComment[]>([]); 
  const [newComment, setNewComment] = useState("");
  const [loading, setLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const ideaIdNum = Number(id);

  const fetchData = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const userProfileStr = localStorage.getItem('user-profile');
      const userProfile = userProfileStr ? JSON.parse(userProfileStr) : {};
      const currentUserId = userProfile.userId;

      const [ideaRes, commentsRes] = await Promise.all([
        getIdeaDetails(ideaIdNum, currentUserId), 
        getComments(ideaIdNum)
      ]);
      
      setIdea(ideaRes.data);
      setComments(Array.isArray(commentsRes.data) ? commentsRes.data : []);
    } catch (err) {
      console.error("Failed to fetch data", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [id]);

  const handleVote = async (type: "UPVOTE" | "DOWNVOTE") => {
    try {
      await postVote(ideaIdNum, { voteType: type });
      fetchData(); 
    } catch (err) {
      console.error("Voting failed", err);
    }
  };

  const handleCommentSubmit = async () => {
    if (!newComment.trim()) return;
    if (!token) {
      toast.error("Authentication token missing. Please log in again.");
      return;
    }

    try {
      setIsSubmitting(true);
      await postComments(ideaIdNum, { text: newComment });
      setNewComment(""); 
      await fetchData(); 
    } catch (err: any) {
      console.error("Comment failed", err);
      toast.error(err.response?.data?.message || "Error: Could not post comment.");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{height: '80vh'}}>
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    );
  }

  if (!idea) {
    return (
      <div className="container mt-5 text-center">
        <h3>Idea not found.</h3>
        <button className="btn btn-primary mt-3 rounded-pill" onClick={() => navigate('/explore')}>
          Back to Explore
        </button>
      </div>
    );
  }

  return (
    <div className="container-fluid my-4 px-lg-5">
      <button className="btn btn-outline-secondary btn-sm mb-4 rounded-pill px-3" onClick={() => navigate(-1)}>
        &larr; Back
      </button>

      <div className="row g-4">
        {/* MAIN COLUMN */}
        <div className="col-lg-8 mx-auto">
          <div className="card border-0 shadow-sm rounded-4 p-4 mb-4">
            <div className="d-flex justify-content-between align-items-start mb-3">
              <span className="badge bg-primary rounded-pill px-3 py-2">
                {idea.category?.name ?? 'General'}
              </span>
              <small className="text-muted">
                {idea.createdAt ? new Date(idea.createdAt).toLocaleDateString() : 'N/A'}
              </small>
            </div>
            
            <h1 className="fw-bold display-6 mb-2">{idea.title}</h1>
            <p className="text-muted mb-3">By {idea.author?.displayName ?? 'Unknown'}</p>

            {idea.thumbnailURL && (
              <div className="mb-4">
                <a href={idea.thumbnailURL} target="_blank" rel="noopener noreferrer" className="btn btn-sm btn-outline-primary rounded-pill d-inline-flex align-items-center gap-1">
                  <Link size={13} /> Attached Link
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
                <p className="text-secondary" style={{ whiteSpace: 'pre-line' }}>
                  {idea.description}
                </p>
              </section>

              {/* VOTE SECTION RELOCATED HERE */}
              <div className="border-top pt-4 mt-4">
                <p className="fw-bold text-muted small text-uppercase mb-3">What do you think of this idea?</p>
                <div className="d-flex gap-2" style={{ maxWidth: '300px' }}>
                  <button 
                    className="btn btn-outline-success btn-sm rounded-pill flex-grow-1 py-2 fw-bold"
                    onClick={() => handleVote("UPVOTE")}
                  >
                    <ThumbsUp size={14} className="me-1" /> {idea.votes?.upvotes ?? 0}
                  </button>
                  <button 
                    className="btn btn-outline-danger btn-sm rounded-pill flex-grow-1 py-2 fw-bold"
                    onClick={() => handleVote("DOWNVOTE")}
                  >
                    <ThumbsDown size={14} className="me-1" /> {idea.votes?.downvotes ?? 0}
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* COMMENTS SECTION */}
          <div className="card border-0 shadow-sm rounded-4 p-4">
            <h5 className="fw-bold mb-4">Discussion ({comments.length})</h5>
            
            <div className="mb-4">
              <textarea 
                className="form-control mb-2 border-0 bg-light rounded-3" 
                placeholder="What are your thoughts on this idea?" 
                rows={3}
                value={newComment}
                disabled={isSubmitting}
                onChange={(e) => setNewComment(e.target.value)}
              />
              <div className="text-end">
                <button 
                  className="btn btn-primary px-4 rounded-pill fw-bold" 
                  onClick={handleCommentSubmit}
                  disabled={isSubmitting || !newComment.trim()}
                >
                  {isSubmitting ? "Posting..." : "Post Comment"}
                </button>
              </div>
            </div>

            <div className="list-group list-group-flush">
              {comments.length > 0 ? (
                comments.map((comment, index) => (
                  <div key={index} className="list-group-item px-0 border-0 mb-3">
                    <div className="d-flex gap-3">
                      <div className="bg-info bg-opacity-10 text-info rounded-circle d-flex align-items-center justify-content-center fw-bold" style={{width:'40px', height:'40px', minWidth: '40px'}}>
                        {comment.displayName?.charAt(0).toUpperCase() || 'U'}
                      </div>
                      <div className="w-100">
                        <div className="bg-light p-3 rounded-4">
                          <p className="mb-1 fw-bold small">{comment.displayName}</p>
                          <p className="text-secondary mb-0 small">{comment.commentText}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-center py-4 text-muted small">
                  No comments yet. Be the first to join the discussion!
                </div>
              )}
            </div>
          </div>
        </div>

        {/* SIDEBAR NOW ONLY CONTAINS STATUS OR CAN BE REMOVED */}

      </div>
    </div>
  );
};

export default NonOwnerIdeaDetail;
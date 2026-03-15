import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  ArrowRight, 
  Layers, 
  Clock, 
  CheckCircle, 
  Users, 
  BarChart3, 
  BoxSelect,
  FileText,
  ShieldCheck,
  ChevronRight,
  ChevronLeft,
  Zap,
  Lightbulb
} from 'lucide-react';
import './LandingPage.css';

const LandingPage: React.FC = () => {
  const [activeStep, setActiveStep] = useState(0);

  const steps = [
    {
      number: "01",
      title: "Draft & Edit",
      description: "Create a new proposal using the standardized templates. Save drafts and iterate before submission."
    },
    {
      number: "02",
      title: "Review",
      description: "Department heads receive notifications. They can request changes, approve, or reject with comments."
    },
    {
      number: "03",
      title: "Proposal",
      description: "Approved proposals move to the implementation track. Milestones are monitored until completion."
    }
  ];

  // Auto-advance carousel
  useEffect(() => {
    const timer = setInterval(() => {
      setActiveStep((prev) => (prev + 1) % steps.length);
    }, 5000);
    return () => clearInterval(timer);
  }, []);

  const nextStep = () => setActiveStep((prev) => (prev + 1) % steps.length);
  const prevStep = () => setActiveStep((prev) => (prev - 1 + steps.length) % steps.length);

  return (
    <div className="landing-page">
      {/* Floating Navigation Island */}
      <nav className="landing-nav">
        <Link to="/" className="landing-brand">
          <div className="app-logo me-2 " style={{backgroundColor: "#3B82F6", color: "white", borderRadius: "10px", padding: "0px 4px", paddingBottom: "2px"}}>
            <Lightbulb size={20} className="fill-current" />
          </div>
          <span>IdeaTrack</span>
        </Link>
        
        <div className="nav-cta-group">
          <Link to="/login" className="btn-minimal">
            Log In
          </Link>
          <Link to="/signup" className="btn-solid">
            Sign Up
          </Link>
        </div>
      </nav>

      {/* Hero Section */}
      <header className="landing-hero">
        <div className="container">
          <div className="hero-content">
            <span className="hero-supertitle">Internal Innovation Platform</span>
            <h1 className="hero-title">
              Where bright ideas <br />
              <span className="text-accent">become projects.</span>
            </h1>
            <p className="hero-subtitle">
              The official system for submitting, tracking, and approving departmental proposals. Streamline your workflow from initial concept to final execution.
            </p>
            <div className="hero-actions">
              <Link to="/signup" className="btn-hero-primary">
                Submit Proposal
              </Link>
              <Link to="/login" className="btn-hero-secondary">
                Track Status
              </Link>
            </div>
          </div>
        </div>
      </header>

      {/* Feature Bento Grid */}
      <section className="bento-section">
        <div className="container">
          <div className="section-header">
            <span className="section-label">System Capabilities</span>
            <h2 className="section-heading">Everything in one place.</h2>
          </div>

          <div className="bento-grid">
            {/* Item 1: Large */}
            <div className="bento-item bento-large">
              <div className="bento-icon">
                <FileText size={24} />
              </div>
              <div className="bento-content">
                <h3 className="bento-title">Standardized Proposals</h3>
                <p className="bento-text">
                  Eliminate email chains and lost documents. Use our structured digital forms to ensure every proposal includes the necessary budget, timeline, and resource requirements before submission.
                </p>
              </div>
            </div>

            {/* Item 2: Tall - Tracking */}
            <div className="bento-item bento-tall bg-light">
              <div className="bento-icon">
                <Clock size={24} />
              </div>
              <div className="bento-content text-start">
                <h3 className="bento-title">Real-time Lifecycle Tracking</h3>
                <p className="bento-text mb-3">
                  Never wonder where your idea stands. Watch your proposal move through stages:
                </p>
                <div className="d-flex flex-column gap-2">
                  <div className="d-flex align-items-center gap-2 opacity-50">
                    <div className="rounded-circle bg-secondary" style={{width: 6, height: 6}}></div>
                    <small style={{fontSize: '0.85rem'}}>Draft</small>
                  </div>
                  <div className="d-flex align-items-center gap-2">
                    <div className="rounded-circle bg-primary" style={{width: 8, height: 8}}></div>
                    <small className="fw-bold text-dark">Under Review</small>
                  </div>
                  <div className="d-flex align-items-center gap-2 opacity-50">
                    <div className="rounded-circle bg-secondary" style={{width: 6, height: 6}}></div>
                    <small style={{fontSize: '0.85rem'}}>Approved</small>
                  </div>
                </div>
              </div>
            </div>

            {/* Item 3: Normal */}
            <div className="bento-item">
              <div className="bento-icon">
                <CheckCircle size={24} />
              </div>
              <div className="bento-content">
                <h3 className="bento-title">Manager Approval</h3>
                <p className="bento-text">
                  One-click sign-offs for department heads. Review budgets and attach feedback directly to the proposal.
                </p>
              </div>
            </div>

            {/* Item 4: Normal */}
            <div className="bento-item">
              <div className="bento-icon">
                <Users size={24} />
              </div>
              <div className="bento-content">
                <h3 className="bento-title">Team Collaboration</h3>
                <p className="bento-text">
                  Invite colleagues to co-author proposals. Comment on specific sections and refine ideas together.
                </p>
              </div>
            </div>

             {/* Item 5: Normal - New Item to fill gap */}
            <div className="bento-item">
               <div className="bento-icon">
                 <Zap size={24} />
               </div>
               <div className="bento-content">
                 <h3 className="bento-title">Instant Notification</h3>
                 <p className="bento-text">
                   Get alerted immediately via email or in-app when your proposal status changes or receives new feedback.
                 </p>
               </div>
             </div>

            {/* Item 6: Large Dark */}
            <div className="bento-item bento-large bento-dark">
              <div className="bento-icon" style={{backgroundColor: 'black', color: 'white', borderColor: 'rgba(255,255,255,0.1)'}}>
                <BarChart3 size={24} />
              </div>
              <div className="bento-content">
                <h3 className="bento-title">Departmental Analytics</h3>
                <p className="bento-text" style={{color: '#94a3b8'}}>
                  Visualize innovation trends across the organization. Track implemented ideas, ROI, and department participation rates through comprehensive dashboards.
                </p>
              </div>
            </div>

            {/* Item 7: Normal - Security */}
             <div className="bento-item">
              <div className="bento-icon">
                <ShieldCheck size={24} />
              </div>
              <div className="bento-content">
                <h3 className="bento-title">Enterprise Security</h3>
                <p className="bento-text">
                  Role-based access controls and encrypted data storage keep sensitive business intelligence protected.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Workflow Steps */}
      <section className="workflow-section">
        <div className="container">
          <div className="row align-items-center gx-5">
            <div className="col-lg-5 mb-5 mb-lg-0">
              <span className="section-label"> Workflow</span>
              <h2 className="section-heading mb-4">How it works.</h2>
              <p className="text-muted" style={{ lineHeight: '1.8' }}>
                A transparent, stage-gate process ensures high-quality proposals get the attention they deserve.
              </p>
              <div className="workflow-nav mt-5 d-flex gap-3">
                 <button onClick={prevStep} className="btn btn-outline-dark rounded-circle p-0 d-flex align-items-center justify-content-center" style={{ width: '48px', height: '48px'} } aria-label="Previous step">
                    <ChevronLeft size={20} />
                 </button>
                 <button onClick={nextStep} className="btn btn-dark rounded-circle p-0 d-flex align-items-center justify-content-center" style={{ width: '48px', height: '48px'} } aria-label="Next step">
                    <ChevronRight size={20} />
                 </button>
              </div>
            </div>
            
            <div className="col-lg-7">
               <div className="workflow-carousel p-5 bg-white rounded-5 shadow-sm border position-relative overflow-hidden" style={{ minHeight: '320px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                  <div key={activeStep} className="workflow-step-content animate-fade-in">
                     <span className="display-1 fw-bold position-absolute top-0 end-0 me-4 mt-2 opacity-10" style={{color: '#000', zIndex: 0}}>
                        {steps[activeStep].number}
                     </span>
                     <div className="position-relative z-1 pe-5">
                        <h3 className="mb-3 fw-bold display-6">{steps[activeStep].title}</h3>
                        <p className="text-muted lead mb-0">{steps[activeStep].description}</p>
                     </div>
                  </div>
                  <div className="d-flex gap-2 mt-5">
                     {steps.map((_, idx) => (
                        <div 
                           key={idx} 
                           className={`rounded-pill transition-all ${idx === activeStep ? 'bg-primary' : 'bg-light'}`}
                           style={{width: idx === activeStep ? '40px' : '10px', height: '6px', transition: 'width 0.3s ease'}}
                        />
                     ))}
                  </div>
               </div>
            </div>
          </div>
        </div>
      </section>

      {/* Modern Footer */}
      <footer className="py-5 bg-white border-top">
        <div className="container">
          <div className="d-flex flex-column flex-md-row justify-content-between align-items-center gap-4">
            <div className="d-flex align-items-center gap-2">
               <div className="footer-logo">
                  <Lightbulb size={20} className="fill-current" />
               </div>
               <span className="fw-bold text-dark">IdeaTrack System</span>
            </div>
            
            <div className="d-flex gap-4 small fw-medium text-muted">
              <Link to="https://www.cognizant.com/us/en/about-cognizant/contact-us" target='_blank' className="footer-link">Contact us</Link>
            </div>

            <div className="small text-muted">
              Internal Use Only • v2.4.0
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;

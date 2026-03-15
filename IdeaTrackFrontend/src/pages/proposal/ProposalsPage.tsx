import { useAuth } from '../../utils/authContext';
import AcceptedIdeasList from '../../components/proposals/AcceptedIdeasList';
import DraftProposalEditor from '../../components/proposals/DraftProposalEditor';
import 'bootstrap/dist/css/bootstrap.min.css';

export default function ProposalsPage() {
  const { payload } = useAuth();
  const USER_ID = (payload as any)?.userId ?? 1;
  const DRAFT_PROPOSAL_ID = 0; // Set to 0 to indicate no draft selected yet

  return (
    <>
      <AcceptedIdeasList userId={USER_ID} />
      {DRAFT_PROPOSAL_ID > 0 && <DraftProposalEditor proposalId={DRAFT_PROPOSAL_ID} />}
    </>
  );
}
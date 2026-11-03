import http from './http'

export interface ChatHistoryItem { role: 'user' | 'assistant'; content: string }
export interface AdvisorProduct {
  id: string; slug: string; name: string; category: string; brand: string;
  price: number; image: string; url: string; stockOnHand: number;
  specs: {label: string}[]; reasons: string[];
  gameFit?: {level: 'minimum' | 'recommended'; cpu: string; gpu: string; ramGb: number; storageGb: number; ssd: boolean;
    cpuComparison: string; gpuComparison: string; ramComparison: string; storageComparison: string};
}
export interface GameHardware {cpu: string; gpu: string; ramGb: number; storageGb: number; ssdRequired: boolean; notes: string}
export interface GameReport {title: string; minimum: GameHardware; recommended: GameHardware;
  sources: {title: string; url: string}[]; checkedAt: string; lookupStatus: 'live_search' | 'verified_reference'; selectedTarget: string}
export interface AdvisorResponse {
  message: string; products: AdvisorProduct[]; model: string | null;
  outcome: 'recommendations' | 'clarification' | 'no_match' | 'out_of_scope';
  suggestions: string[];
  criteria: { groups: string[]; maxBudget: number | null; purpose: string | null };
  game?: GameReport | null;
}
export interface AdvisorStatus {configured: boolean; model: string; maxMessageLength: number; maxTurns: number}

export async function getAdvisorStatus(signal: AbortSignal): Promise<AdvisorStatus> {
  const response = await http.get('/ai/status', {signal, timeout: 8000})
  return response.data
}

export async function sendAdvisorMessage(message: string, history: ChatHistoryItem[], signal: AbortSignal): Promise<AdvisorResponse> {
  const csrf = await http.get('/csrf', {signal,timeout:8000})
  const response = await http.post('/ai/chat', {message,history}, {
    signal, timeout:65000, withXSRFToken:false,
    headers: {[csrf.data.headerName || 'X-XSRF-TOKEN']: csrf.data.token},
  })
  return response.data
}

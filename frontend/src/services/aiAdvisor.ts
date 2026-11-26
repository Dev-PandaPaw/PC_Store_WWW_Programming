import http from './http'

export interface ChatHistoryItem { role: 'user' | 'assistant'; content: string }
export interface AdvisorProduct {
  id: string; slug: string; name: string; category: string; brand: string;
  price: number; image: string; url: string; stockOnHand: number;
  specs: {label: string}[]; reasons: string[];
  demoCatalog?: boolean;
  hardwareFit?: {status: 'needs_verification'; checks: string[]};
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
  build?: ComponentBuild | null;
}
export interface ComponentBuild {
  status: 'compatible' | 'needs_verification' | 'no_match';
  total: number; budget: number; retrieval: 'vector';
  items: {category: string; label: string; productId: string; quantity: number}[];
  checks: {label: string; status: 'passed' | 'needs_verification'; detail: string}[];
  missingCategories: string[];
}
export interface AdvisorStatus {configured: boolean; model: string; maxMessageLength: number; maxTurns: number}
export interface HardwarePart {
  category: string; model: string; socket: string; memoryType: '' | 'DDR3' | 'DDR4' | 'DDR5';
  capacityGb: number; wattageW: number; confidence: 'high' | 'low';
}
export interface HardwareContext {use: 'current' | 'desired'; components: HardwarePart[]; filename: string}
export interface HardwareExtraction {readable: boolean; components: HardwarePart[]; notes: string[]}

export async function analyzeHardwareAttachment(file: File, signal: AbortSignal): Promise<HardwareExtraction> {
  const csrf=await http.get('/csrf',{signal,timeout:8000})
  const form=new FormData();form.append('file',file)
  const response=await http.post('/ai/attachments/analyze',form,{signal,timeout:45000,withXSRFToken:false,
    headers:{'Content-Type':undefined,[csrf.data.headerName||'X-XSRF-TOKEN']:csrf.data.token}})
  return response.data
}

export async function getAdvisorStatus(signal: AbortSignal): Promise<AdvisorStatus> {
  const response = await http.get('/ai/status', {signal, timeout: 8000})
  return response.data
}

export async function sendAdvisorMessage(message: string, history: ChatHistoryItem[], signal: AbortSignal, hardware?: HardwareContext): Promise<AdvisorResponse> {
  const csrf = await http.get('/csrf', {signal,timeout:8000})
  const response = await http.post('/ai/chat', {message,history,...(hardware?{hardware}:{})}, {
    signal, timeout:65000, withXSRFToken:false,
    headers: {[csrf.data.headerName || 'X-XSRF-TOKEN']: csrf.data.token},
  })
  return response.data
}

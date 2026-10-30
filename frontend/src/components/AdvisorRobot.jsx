import { useEffect, useId, useRef } from 'react'
import '../styles/advisor-robot.css'

// SVG geometry and animation adapted from the supplied transparent robot component.
export default function AdvisorRobot() {
  const id = useId()
  const waveArm = useRef(null)
  const waveAnimation = useRef(null)
  useEffect(() => () => waveAnimation.current?.cancel(), [])

  function wave() {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches || waveAnimation.current?.playState === 'running') return
    // A separate joint keeps the idle animation running without jumping back to its first frame.
    waveAnimation.current = waveArm.current?.animate([
      { transform: 'rotate(0deg)' },
      { transform: 'rotate(-9deg)' },
      { transform: 'rotate(7deg)' },
      { transform: 'rotate(-6deg)' },
      { transform: 'rotate(0deg)' },
    ], { duration: 1600, easing: 'ease-in-out' })
  }

  return <aside className="ai-robot-companion" aria-label="Robot PixelCore">
    <button type="button" className="ai-robot anim-idle anim-wave anim-blink"
      onClick={wave} aria-label="Chào robot PixelCore" title="Nhấn để robot vẫy chào">
<svg viewBox="-70 -20 760 920" className="ai-robot__art" aria-hidden="true" focusable="false" xmlns="http://www.w3.org/2000/svg">
                <defs>
                    <linearGradient id={`${id}-whiteArmor`} x1="15%" y1="10%" x2="85%" y2="90%">
                        <stop offset="0%" stopColor="#ffffff"></stop>
                        <stop offset="55%" stopColor="#f1f5f9"></stop>
                        <stop offset="85%" stopColor="#cbd5e1"></stop>
                        <stop offset="100%" stopColor="#94a3b8"></stop>
                    </linearGradient>
                    <linearGradient id={`${id}-whiteArmorLight`} x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stopColor="#ffffff"></stop>
                        <stop offset="70%" stopColor="#e2e8f0"></stop>
                        <stop offset="100%" stopColor="#bae6fd"></stop>
                    </linearGradient>
                    <linearGradient id={`${id}-darkJoint`} x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stopColor="#334155"></stop>
                        <stop offset="45%" stopColor="#0f172a"></stop>
                        <stop offset="100%" stopColor="#020617"></stop>
                    </linearGradient>
                    <linearGradient id={`${id}-carbonAbdomen`} x1="0%" y1="0%" x2="100%" y2="0%">
                        <stop offset="0%" stopColor="#090d16"></stop>
                        <stop offset="50%" stopColor="#1e293b"></stop>
                        <stop offset="100%" stopColor="#090d16"></stop>
                    </linearGradient>
                    <radialGradient id={`${id}-visorScreen`} cx="50%" cy="45%" r="58%">
                        <stop offset="0%" stopColor="#0a2239"></stop>
                        <stop offset="65%" stopColor="#040d1a"></stop>
                        <stop offset="100%" stopColor="#010409"></stop>
                    </radialGradient>
                    <linearGradient id={`${id}-cyanLed`} x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stopColor="#7ef9ff"></stop>
                        <stop offset="50%" stopColor="#00f2fe"></stop>
                        <stop offset="100%" stopColor="#00a8ff"></stop>
                    </linearGradient>
                    <radialGradient id={`${id}-floorShadowGrad`} cx="50%" cy="50%" r="50%">
                        <stop offset="0%" stopColor="#0f172a" stopOpacity="0.38"></stop>
                        <stop offset="55%" stopColor="#0284c7" stopOpacity="0.12"></stop>
                        <stop offset="100%" stopColor="#0f172a" stopOpacity="0"></stop>
                    </radialGradient>
                    <filter id={`${id}-armorShadow`} x="-10%" y="-10%" width="125%" height="125%">
                        <feDropShadow dx="0" dy="4" stdDeviation="5" floodColor="#0f172a" floodOpacity="0.16"></feDropShadow>
                    </filter>
                </defs>
                <ellipse data-robot-part="floor-shadow" cx="300" cy="852" rx="145" ry="20" fill={`url(#${id}-floorShadowGrad)`}></ellipse>
                <g data-robot-part="lower-body">
                    <g data-robot-part="viewer-left-leg">
                        <circle cx="255" cy="495" r="28" fill={`url(#${id}-darkJoint)`}></circle>
                        <path d="M 226 485 C 212 535, 206 585, 212 630 C 218 645, 256 645, 266 630 C 276 585, 284 535, 282 495 Z" fill={`url(#${id}-whiteArmor)`} filter={`url(#${id}-armorShadow)`}></path>
                        <ellipse cx="222" cy="512" rx="6" ry="15" transform="rotate(14 222 512)" fill={`url(#${id}-darkJoint)`} stroke={`url(#${id}-cyanLed)`} strokeWidth="2.5" className="led-pulse"></ellipse>
                        <path d="M 255 545 L 244 612" stroke={`url(#${id}-cyanLed)`} strokeWidth="3" strokeLinecap="round" className="led-pulse"></path>
                        <circle cx="238" cy="638" r="24" fill={`url(#${id}-darkJoint)`}></circle>
                        <circle cx="256" cy="638" r="10" fill="#050b14" stroke={`url(#${id}-cyanLed)`} strokeWidth="3.5" className="led-pulse"></circle>
                        <path d="M 216 626 C 216 610, 244 610, 244 626 C 244 646, 216 646, 216 626 Z" fill={`url(#${id}-whiteArmorLight)`}></path>
                        <path d="M 212 648 C 194 695, 196 748, 204 785 C 215 798, 250 798, 256 785 C 266 742, 268 695, 258 650 Z" fill={`url(#${id}-whiteArmor)`} filter={`url(#${id}-armorShadow)`}></path>
                        <path d="M 226 675 L 219 765" stroke={`url(#${id}-cyanLed)`} strokeWidth="3.5" strokeLinecap="round" className="led-pulse"></path>
                        <circle cx="228" cy="792" r="22" fill={`url(#${id}-darkJoint)`}></circle>
                        <circle cx="228" cy="792" r="12" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="2.5" className="led-pulse"></circle>
                        <path d="M 192 805 C 182 818, 176 836, 182 846 C 192 856, 262 856, 270 846 C 276 834, 266 812, 256 802 Z" fill={`url(#${id}-darkJoint)`}></path>
                        <path d="M 196 796 C 186 812, 182 830, 190 838 C 202 846, 252 846, 262 838 C 268 828, 260 806, 250 796 Z" fill={`url(#${id}-whiteArmor)`}></path>
                        <path d="M 214 816 L 238 816" stroke={`url(#${id}-cyanLed)`} strokeWidth="4" strokeLinecap="round" className="led-pulse"></path>
                        <path d="M 184 842 Q 226 852 268 842" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="4.5" strokeLinecap="round" className="led-pulse cyan-glow"></path>
                    </g>
                    <g data-robot-part="viewer-right-leg">
                        <circle cx="345" cy="495" r="28" fill={`url(#${id}-darkJoint)`}></circle>
                        <path d="M 318 495 C 316 535, 324 585, 334 630 C 344 645, 382 645, 388 630 C 394 585, 388 535, 374 485 Z" fill={`url(#${id}-whiteArmor)`} filter={`url(#${id}-armorShadow)`}></path>
                        <path d="M 346 545 L 356 612" stroke={`url(#${id}-cyanLed)`} strokeWidth="3" strokeLinecap="round" className="led-pulse"></path>
                        <circle cx="362" cy="638" r="24" fill={`url(#${id}-darkJoint)`}></circle>
                        <circle cx="342" cy="638" r="12" fill="#050b14" stroke={`url(#${id}-cyanLed)`} strokeWidth="3.5" className="led-pulse cyan-glow"></circle>
                        <path d="M 356 626 C 356 610, 384 610, 384 626 C 384 646, 356 646, 356 626 Z" fill={`url(#${id}-whiteArmorLight)`}></path>
                        <path d="M 342 650 C 332 695, 334 742, 344 785 C 350 798, 385 798, 396 785 C 404 748, 406 695, 388 648 Z" fill={`url(#${id}-whiteArmor)`} filter={`url(#${id}-armorShadow)`}></path>
                        <path d="M 374 675 L 382 765" stroke={`url(#${id}-cyanLed)`} strokeWidth="3.5" strokeLinecap="round" className="led-pulse"></path>
                        <circle cx="368" cy="792" r="23" fill={`url(#${id}-darkJoint)`}></circle>
                        <circle cx="352" cy="794" r="13" fill="#050b14" stroke={`url(#${id}-cyanLed)`} strokeWidth="3.5" className="led-pulse cyan-glow"></circle>
                        <path d="M 332 806 C 325 820, 326 838, 338 844 C 365 852, 438 854, 456 842 C 464 832, 452 816, 428 808 L 386 792 Z" fill={`url(#${id}-darkJoint)`}></path>
                        <path d="M 344 798 C 340 812, 342 826, 354 832 C 378 838, 430 838, 446 828 C 450 818, 432 804, 405 796 Z" fill={`url(#${id}-whiteArmor)`}></path>
                        <path d="M 404 808 L 424 812" stroke={`url(#${id}-cyanLed)`} strokeWidth="4" strokeLinecap="round" className="led-pulse"></path>
                        <path d="M 332 836 C 362 846, 415 848, 454 836" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="5" strokeLinecap="round" className="led-pulse cyan-glow"></path>
                    </g>
                    <g data-robot-part="pelvis-armor">
                        <path d="M 235 452 L 365 452 L 352 515 L 300 535 L 248 515 Z" fill={`url(#${id}-darkJoint)`}></path>
                        <path d="M 232 456 C 265 472, 335 472, 368 456 C 364 492, 338 518, 300 532 C 262 518, 236 492, 232 456 Z" fill={`url(#${id}-whiteArmor)`} filter={`url(#${id}-armorShadow)`}></path>
                        <path d="M 242 468 Q 300 492 358 468" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="3" strokeLinecap="round" className="led-pulse"></path>
                    </g>
                </g>
                <g data-robot-part="upper-body">
                    <g data-robot-part="viewer-left-arm">
                        <circle cx="215" cy="318" r="30" fill={`url(#${id}-darkJoint)`}></circle>
                        <circle cx="215" cy="318" r="22" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="4" className="led-pulse cyan-glow"></circle>
                        <path d="M 184 312 C 162 342, 152 385, 154 426 C 164 438, 194 438, 202 424 C 210 388, 218 352, 212 320 Z" fill={`url(#${id}-whiteArmor)`} filter={`url(#${id}-armorShadow)`}></path>
                        <ellipse cx="182" cy="336" rx="12" ry="22" transform="rotate(20 182 336)" fill={`url(#${id}-darkJoint)`} stroke={`url(#${id}-cyanLed)`} strokeWidth="3.5" className="led-pulse"></ellipse>
                        <g data-robot-part="viewer-left-forearm">
                            <circle cx="174" cy="435" r="20" fill={`url(#${id}-darkJoint)`}></circle>
                            <circle cx="174" cy="435" r="9" fill="#050b14" stroke={`url(#${id}-cyanLed)`} strokeWidth="3" className="led-pulse"></circle>
                            <path d="M 152 444 C 138 482, 142 528, 156 562 C 168 570, 192 566, 196 552 C 198 515, 196 476, 190 444 Z" fill={`url(#${id}-whiteArmor)`} filter={`url(#${id}-armorShadow)`}></path>
                            <path d="M 164 462 Q 155 508 168 548" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="3.5" strokeLinecap="round" className="led-pulse cyan-glow"></path>
                            <circle cx="176" cy="564" r="14" fill={`url(#${id}-darkJoint)`} stroke={`url(#${id}-cyanLed)`} strokeWidth="2.5" className="led-pulse"></circle>
                            <g data-robot-part="viewer-left-hand">
                                <path d="M 160 570 C 154 588, 158 608, 172 615 C 186 615, 194 598, 192 574 Z" fill={`url(#${id}-whiteArmor)`}></path>
                                <path d="M 188 578 C 202 588, 206 604, 198 612 C 192 612, 186 598, 184 586 Z" fill={`url(#${id}-darkJoint)`}></path>
                                <path d="M 160 606 C 164 624, 176 634, 186 630 C 190 622, 182 610, 174 604 Z" fill={`url(#${id}-darkJoint)`}></path>
                            </g>
                        </g>
                    </g>
                    <g data-robot-part="torso-core">
                        <path d="M 274 248 L 326 248 L 334 292 L 266 292 Z" fill={`url(#${id}-darkJoint)`}></path>
                        <path d="M 286 254 L 283 286" stroke={`url(#${id}-cyanLed)`} strokeWidth="3.5" strokeLinecap="round" className="led-pulse cyan-glow"></path>
                        <path d="M 314 254 L 317 286" stroke={`url(#${id}-cyanLed)`} strokeWidth="3.5" strokeLinecap="round" className="led-pulse cyan-glow"></path>
                        <ellipse cx="300" cy="288" rx="44" ry="12" fill="#090d16" stroke={`url(#${id}-cyanLed)`} strokeWidth="2.5" className="led-pulse"></ellipse>
                        <path d="M 228 368 C 242 405, 246 436, 240 462 L 360 462 C 354 436, 358 405, 372 368 Z" fill={`url(#${id}-carbonAbdomen)`}></path>
                        <path d="M 248 392 C 264 418, 272 440, 275 460" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="3.5" strokeLinecap="round" className="led-pulse cyan-glow"></path>
                        <path d="M 352 392 C 336 418, 328 440, 325 460" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="3.5" strokeLinecap="round" className="led-pulse cyan-glow"></path>
                        <path d="M 212 292 C 245 276, 355 276, 388 292 C 404 328, 392 378, 362 396 C 335 376, 265 376, 238 396 C 208 378, 196 328, 212 292 Z" fill={`url(#${id}-whiteArmor)`} filter={`url(#${id}-armorShadow)`}></path>
                        <path d="M 252 288 Q 300 308 348 288" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="4" strokeLinecap="round" className="led-pulse cyan-glow"></path>
                        <path d="M 222 342 L 265 366 L 260 390" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="4" strokeLinecap="round" strokeLinejoin="round" className="led-pulse cyan-glow"></path>
                        <path d="M 378 342 L 335 366 L 340 390" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="4" strokeLinecap="round" strokeLinejoin="round" className="led-pulse cyan-glow"></path>
                    </g>
                    <g ref={waveArm} data-robot-part="greeting-wave">
                    <g data-robot-part="viewer-right-arm">
                        <circle cx="385" cy="318" r="30" fill={`url(#${id}-darkJoint)`}></circle>
                        <circle cx="385" cy="318" r="22" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="4" className="led-pulse cyan-glow"></circle>
                        <path d="M 392 302 C 424 324, 452 362, 466 396 C 458 416, 432 424, 418 412 C 396 385, 382 352, 376 324 Z" fill={`url(#${id}-whiteArmor)`} filter={`url(#${id}-armorShadow)`}></path>
                        <path d="M 404 316 Q 426 334 436 355" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="3" strokeLinecap="round" className="led-pulse"></path>
                        <g data-robot-part="viewer-right-forearm">
                            <circle cx="455" cy="410" r="22" fill={`url(#${id}-darkJoint)`}></circle>
                            <circle cx="458" cy="414" r="12" fill="#050b14" stroke={`url(#${id}-cyanLed)`} strokeWidth="3.5" className="led-pulse cyan-glow"></circle>
                            <path d="M 444 394 C 468 364, 496 338, 522 322 C 538 328, 544 346, 534 360 C 510 385, 486 410, 468 422 Z" fill={`url(#${id}-whiteArmor)`} filter={`url(#${id}-armorShadow)`}></path>
                            <path d="M 476 388 L 518 346" stroke={`url(#${id}-cyanLed)`} strokeWidth="3" strokeLinecap="round" className="led-pulse"></path>
                            <circle cx="532" cy="332" r="14" fill={`url(#${id}-darkJoint)`} stroke={`url(#${id}-cyanLed)`} strokeWidth="2.5" className="led-pulse"></circle>
                            <g data-robot-part="viewer-right-hand">
                                <path d="M 526 322 C 528 302, 542 286, 552 284 C 558 290, 550 308, 540 322 Z" fill={`url(#${id}-darkJoint)`}></path>
                                <path d="M 528 320 C 530 308, 538 298, 544 296" fill="none" stroke="#e2e8f0" strokeWidth="4" strokeLinecap="round"></path>
                                <path d="M 548 318 C 566 304, 584 294, 592 298 C 594 304, 576 318, 556 328 Z" fill={`url(#${id}-darkJoint)`}></path>
                                <path d="M 552 326 C 572 314, 592 306, 598 312 C 598 318, 578 330, 558 336 Z" fill={`url(#${id}-darkJoint)`}></path>
                                <path d="M 554 334 C 572 326, 588 320, 594 326 C 592 332, 574 340, 556 344 Z" fill={`url(#${id}-darkJoint)`}></path>
                                <path d="M 526 324 C 542 316, 562 320, 568 334 C 562 348, 542 350, 528 340 Z" fill={`url(#${id}-whiteArmor)`} filter={`url(#${id}-armorShadow)`}></path>
                                <circle cx="546" cy="334" r="3.5" fill={`url(#${id}-cyanLed)`} className="led-pulse"></circle>
                            </g>
                        </g>
                    </g>
                    </g>
                    <g data-robot-part="head-group">
                        <g data-robot-part="left-ear">
                            <ellipse cx="166" cy="178" rx="22" ry="48" transform="rotate(-5 166 178)" fill={`url(#${id}-darkJoint)`}></ellipse>
                            <ellipse cx="174" cy="178" rx="18" ry="44" transform="rotate(-5 174 178)" fill={`url(#${id}-whiteArmor)`}></ellipse>
                            <ellipse cx="162" cy="178" rx="14" ry="38" transform="rotate(-5 162 178)" fill="#050b14" stroke={`url(#${id}-cyanLed)`} strokeWidth="4.5" className="led-pulse cyan-glow"></ellipse>
                            <ellipse cx="160" cy="178" rx="7" ry="22" transform="rotate(-5 160 178)" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="2.5" className="led-pulse"></ellipse>
                        </g>
                        <g data-robot-part="right-ear">
                            <ellipse cx="434" cy="178" rx="22" ry="48" transform="rotate(5 434 178)" fill={`url(#${id}-darkJoint)`}></ellipse>
                            <ellipse cx="426" cy="178" rx="18" ry="44" transform="rotate(5 426 178)" fill={`url(#${id}-whiteArmor)`}></ellipse>
                            <ellipse cx="438" cy="178" rx="14" ry="38" transform="rotate(5 438 178)" fill="#050b14" stroke={`url(#${id}-cyanLed)`} strokeWidth="4.5" className="led-pulse cyan-glow"></ellipse>
                            <ellipse cx="440" cy="178" rx="7" ry="22" transform="rotate(5 440 178)" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="2.5" className="led-pulse"></ellipse>
                        </g>
                        <path d="M 168 165 C 168 68, 432 68, 432 165 C 432 238, 385 266, 300 266 C 215 266, 168 238, 168 165 Z" fill={`url(#${id}-whiteArmor)`} filter={`url(#${id}-armorShadow)`}></path>
                        <ellipse cx="290" cy="96" rx="75" ry="20" fill="#ffffff" opacity="0.65"></ellipse>
                        <path d="M 228 82 C 245 92, 256 106, 262 122" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="5" strokeLinecap="round" className="led-pulse cyan-glow"></path>
                        <path d="M 372 82 C 355 92, 344 106, 338 122" fill="none" stroke={`url(#${id}-cyanLed)`} strokeWidth="5" strokeLinecap="round" className="led-pulse cyan-glow"></path>
                        <rect x="192" y="122" width="216" height="122" rx="48" ry="48" fill={`url(#${id}-darkJoint)`} stroke={`url(#${id}-cyanLed)`} strokeWidth="2.5" className="led-pulse"></rect>
                        <rect x="197" y="127" width="206" height="112" rx="44" ry="44" fill={`url(#${id}-visorScreen)`}></rect>
                        <ellipse cx="300" cy="185" rx="85" ry="40" fill="#00f2fe" opacity="0.08"></ellipse>
                        <path d="M 215 138 Q 290 132 365 142" fill="none" stroke="#ffffff" strokeWidth="3" strokeLinecap="round" opacity="0.18"></path>
                        <g data-robot-part="face-expressions" className="cyan-glow-intense">
                            <g className="eye-left">
                                <path data-robot-part="left-eye-path" d="M 234 182 C 242 158, 270 158, 278 182" fill="none" stroke="#00ffff" strokeWidth="10" strokeLinecap="round"></path>
                                <path d="M 234 182 C 242 158, 270 158, 278 182" fill="none" stroke="#ffffff" strokeWidth="3.5" strokeLinecap="round" opacity="0.8"></path>
                            </g>
                            <g className="eye-right">
                                <path data-robot-part="right-eye-path" d="M 322 182 C 330 158, 358 158, 366 182" fill="none" stroke="#00ffff" strokeWidth="10" strokeLinecap="round"></path>
                                <path d="M 322 182 C 330 158, 358 158, 366 182" fill="none" stroke="#ffffff" strokeWidth="3.5" strokeLinecap="round" opacity="0.8"></path>
                            </g>
                            <g data-robot-part="mouth-shape">
                                <path data-robot-part="mouth-path" d="M 284 205 Q 300 218 316 205" fill="none" stroke="#00ffff" strokeWidth="6" strokeLinecap="round"></path>
                                <path d="M 284 205 Q 300 218 316 205" fill="none" stroke="#ffffff" strokeWidth="2" strokeLinecap="round" opacity="0.75"></path>
                            </g>
                        </g>
                    </g>
                </g>
            </svg>
    </button>
  </aside>
}

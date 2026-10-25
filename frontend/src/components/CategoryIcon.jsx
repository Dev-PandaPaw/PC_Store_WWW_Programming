import React from 'react'
import {
  Gamepad2,
  Monitor,
  Cpu,
  CircuitBoard,
  MemoryStick,
  HardDrive,
  Fan,
  PcCase,
  Zap,
  Keyboard,
  Mouse,
} from 'lucide-react'

const icons = {
  Gamepad2,
  Monitor,
  Cpu,
  CircuitBoard,
  MemoryStick,
  HardDrive,
  Fan,
  PcCase,
  Zap,
  Keyboard,
  Mouse,
}

export default function CategoryIcon({ name, ...props }) {
  const Icon = icons[name] || Cpu
  return <Icon size={19} strokeWidth={1.7} aria-hidden="true" {...props} />
}

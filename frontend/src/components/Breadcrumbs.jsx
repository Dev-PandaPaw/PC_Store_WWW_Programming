import React from 'react'
import { Link } from 'react-router-dom'
import { ChevronsRight, House } from 'lucide-react'
import './Breadcrumbs.css'

export default function Breadcrumbs({ items = [], className = '', style }) {
  return (
    <nav className={`breadcrumbs ${className}`.trim()} aria-label="Đường dẫn" style={style}>
      <ol className="breadcrumbs__list">
        <li className="breadcrumbs__item breadcrumbs__item--home">
          <Link className="breadcrumbs__home" to="/"><House size={17} aria-hidden="true" /><span>Trang chủ</span></Link>
        </li>
        {items.map((item, index) => (
          <li className="breadcrumbs__item" key={`${item.to || 'current'}-${index}`}>
            <ChevronsRight className="breadcrumbs__separator" size={16} aria-hidden="true" />
            {item.to && index < items.length - 1
              ? <Link to={item.to}>{item.label}</Link>
              : <span aria-current={index === items.length - 1 ? 'page' : undefined}>{item.label}</span>}
          </li>
        ))}
      </ol>
    </nav>
  )
}

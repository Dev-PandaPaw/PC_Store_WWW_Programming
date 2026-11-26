import { createContext, useContext } from 'react'

export const CatalogContext = createContext(null)

export function useCatalog() {
  return useContext(CatalogContext)
}

import { Loader2 } from 'lucide-react'

// Indicador de carregamento, usando o icone de loader do lucide girando.
export default function Spinner() {
  return (
    <div className="flex justify-center py-20" aria-label="Carregando">
      <Loader2 className="h-8 w-8 text-accent animate-spin" />
    </div>
  )
}

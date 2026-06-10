import { HTMLContainer, BaseBoxShapeUtil } from 'tldraw'

export class PhetShapeUtil extends BaseBoxShapeUtil {
  static type = 'phet'

  getDefaultProps() {
    return {
      w: 800,
      h: 600,
      url: 'https://phet.colorado.edu/sims/html/energy-skate-park/latest/energy-skate-park_all.html'
    }
  }

  canEdit() { return false } // No inline edit needed, it's just an iframe
  canResize() { return true }

  indicator(shape) {
    return <rect width={shape.props.w} height={shape.props.h} />
  }

  getIndicatorPath(shape) {
    const path = new Path2D()
    path.rect(0, 0, shape.props.w, shape.props.h)
    return path
  }

  component(shape) {
    try {
      
      return (
        <HTMLContainer
          id={shape.id}
          style={{
            pointerEvents: 'none',
            backgroundColor: 'white',
            border: '2px solid #ccc',
            borderRadius: '8px',
            overflow: 'hidden',
            boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
            display: 'flex',
            flexDirection: 'column'
          }}
        >
          {/* Một thanh header nhỏ phía trên để kéo thả shape khi nhúng iframe */}
          <div style={{
            height: '30px', 
            backgroundColor: '#f1f5f9', 
            borderBottom: '1px solid #ccc',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0 10px',
            fontSize: '12px',
            fontWeight: 'bold',
            color: '#475569',
            pointerEvents: 'none'
          }}>
            <span>🧪 Phòng Thí Nghiệm PhET</span>
            <button 
              style={{
                pointerEvents: 'all',
                background: 'transparent',
                border: 'none',
                color: '#ef4444',
                fontWeight: 'bold',
                cursor: 'pointer',
                padding: '2px 5px',
                borderRadius: '4px'
              }}
              onClick={() => this.editor.deleteShape(shape.id)}
              title="Xóa Thí Nghiệm"
            >
              ✕
            </button>
          </div>
          <iframe 
            src={shape.props.url}
            style={{ width: '100%', flex: 1, border: 'none', pointerEvents: 'all' }}
            allowFullScreen
          />
        </HTMLContainer>
      )
    } catch (err) {
      return <HTMLContainer id={shape.id}>Lỗi PhetShape: {err.message}</HTMLContainer>
    }
  }
}

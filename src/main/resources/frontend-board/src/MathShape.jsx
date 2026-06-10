import { HTMLContainer, BaseBoxShapeUtil } from 'tldraw'
import { useRef, useEffect } from 'react'
import katex from 'katex'
import 'katex/dist/katex.min.css'

export class MathShapeUtil extends BaseBoxShapeUtil {
  static type = 'math'

  getDefaultProps() {
    return {
      w: 200,
      h: 100,
      latex: 'x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}',
    }
  }

  canEdit() { return true }
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
      const isEditing = this.editor.getEditingShapeId() === shape.id
      const mathFieldRef = useRef(null)
      const scale = Math.min(shape.props.w / 200, shape.props.h / 100)

      useEffect(() => {
        const mf = mathFieldRef.current
        if (mf) {
          mf.value = shape.props.latex
          const onInput = (e) => {
            this.editor.updateShape({
              id: shape.id,
              type: 'math',
              props: { latex: e.target.value }
            })
          }
          mf.addEventListener('input', onInput)
          return () => mf.removeEventListener('input', onInput)
        }
      }, [isEditing, shape.id]) // Gắn sự kiện khi ở chế độ Edit

      const innerStyle = {
        width: '200px',
        height: '100px',
        transform: `scale(${scale})`,
        transformOrigin: 'center',
        position: 'absolute',
        top: '50%',
        left: '50%',
        marginLeft: '-100px',
        marginTop: '-50px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }

      if (isEditing) {
        return (
          <HTMLContainer id={shape.id} style={{ pointerEvents: 'all', position: 'relative' }}>
            <div style={innerStyle}>
              <math-field 
                ref={mathFieldRef} 
                style={{ fontSize: '24px', width: '100%', height: '100%', backgroundColor: 'white', borderRadius: '4px', border: '1px solid #ccc' }}
              >
                {shape.props.latex}
              </math-field>
            </div>
          </HTMLContainer>
        )
      }

      let html = ''
      try {
        html = katex.renderToString(shape.props.latex || '', {
          throwOnError: false,
          displayMode: true,
        })
      } catch (e) {
        html = `<div style="color: red;">Error: ${e.message}</div>`
      }

      return (
        <HTMLContainer
          id={shape.id}
          style={{
            pointerEvents: 'none',
            backgroundColor: 'transparent',
            position: 'relative'
          }}
        >
          <div style={innerStyle}>
            <div dangerouslySetInnerHTML={{ __html: html }} style={{ fontSize: '24px', width: '100%', textAlign: 'center' }} />
          </div>
        </HTMLContainer>
      )
    } catch (err) {
      return <HTMLContainer id={shape.id}>Lỗi MathShape: {err.message}</HTMLContainer>
    }
  }
}

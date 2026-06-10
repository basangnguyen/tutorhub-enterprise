import { HTMLContainer, BaseBoxShapeUtil, Rectangle2d } from 'tldraw'
import { useEffect, useState, useCallback } from 'react'

export class QuizShapeUtil extends BaseBoxShapeUtil {
  static type = 'quiz'

  getDefaultProps() {
    return {
      w: 500,
      h: 460,
      question: 'Câu hỏi trắc nghiệm...',
      options: ['Đáp án A', 'Đáp án B', 'Đáp án C', 'Đáp án D'],
      status: 'editing', // 'editing', 'voting', 'finished'
      // Không lưu votes trong props để tránh xung đột khi nhiều người bấm cùng lúc
    }
  }

  getGeometry(shape) {
    return new Rectangle2d({
      width: shape.props.w,
      height: shape.props.h,
      isFilled: true,
    })
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
      const scale = Math.min(shape.props.w / 500, shape.props.h / 460)
      
      const { question, options, status } = shape.props;
      const [votes, setVotes] = useState({});

      // Lắng nghe sự kiện bầu chọn từ LiveKit (thông qua window dispatchEvent)
      useEffect(() => {
        if (status !== 'voting' && status !== 'finished') return;

        const handleVote = (e) => {
          const data = e.detail;
          if (data && data.shapeId === shape.id) {
            setVotes(prev => {
              const newVotes = { ...prev };
              newVotes[data.option] = (newVotes[data.option] || 0) + 1;
              return newVotes;
            });
          }
        };

        window.addEventListener('quiz_vote_received', handleVote);
        return () => window.removeEventListener('quiz_vote_received', handleVote);
      }, [status, shape.id]);

      const onQuestionChange = (e) => {
        this.editor.updateShape({
          id: shape.id,
          type: 'quiz',
          props: { question: e.target.value }
        });
      };

      const onOptionChange = (index, value) => {
        const newOptions = [...options];
        newOptions[index] = value;
        this.editor.updateShape({
          id: shape.id,
          type: 'quiz',
          props: { options: newOptions }
        });
      };

      const startVoting = () => {
        setVotes({});
        this.editor.updateShape({
          id: shape.id,
          type: 'quiz',
          props: { status: 'voting' }
        });
      };

      const stopVoting = () => {
        this.editor.updateShape({
          id: shape.id,
          type: 'quiz',
          props: { status: 'finished' }
        });
      };

      const handleVoteClick = (index) => {
        if (window.sendQuizVote) {
          window.sendQuizVote(shape.id, index);
        }
      };

      const innerStyle = {
        width: '500px',
        height: '460px',
        transform: `scale(${scale})`,
        transformOrigin: 'center',
        position: 'absolute',
        top: '50%',
        left: '50%',
        marginLeft: '-250px',
        marginTop: '-230px',
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: '#fff',
        borderRadius: '12px',
        boxShadow: '0 10px 25px rgba(0,0,0,0.2)',
        overflow: 'hidden',
        fontFamily: 'sans-serif',
        pointerEvents: 'none', // Allow dragging on empty spaces
      };

      const headerStyle = {
        background: 'linear-gradient(135deg, #6366f1, #a855f7)',
        color: 'white',
        padding: '12px 15px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        fontWeight: 'bold',
        fontSize: '18px',
        pointerEvents: 'none'
      };

      return (
        <HTMLContainer
          id={shape.id}
          style={{
            pointerEvents: 'none',
            position: 'relative'
          }}
        >
          <div style={innerStyle}>
            
            <div style={headerStyle}>
              <span>{status === 'editing' ? 'Tạo Câu hỏi Trắc nghiệm' : (status === 'voting' ? 'Đang Bình Chọn...' : 'Kết Quả Trắc Nghiệm')}</span>
              <button 
                style={{ background: 'rgba(255,255,255,0.2)', color: 'white', border: 'none', borderRadius: '50%', width: '28px', height: '28px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', pointerEvents: 'all' }}
                onClick={(e) => { e.stopPropagation(); this.editor.deleteShape(shape.id); }}
                title="Xóa Quiz"
              >
                ✕
              </button>
            </div>

            <div style={{ padding: '20px', flex: 1, display: 'flex', flexDirection: 'column', gap: '15px' }}>
              {status === 'editing' && (
                <>
                  <textarea 
                    value={question} 
                    onChange={onQuestionChange}
                    placeholder="Nhập nội dung câu hỏi..."
                    style={{ pointerEvents: 'all', width: '100%', height: '80px', padding: '10px', borderRadius: '8px', border: '1px solid #ccc', resize: 'none', fontSize: '15px', boxSizing: 'border-box' }}
                  />
                  {options.map((opt, i) => (
                    <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <span style={{ fontWeight: 'bold', color: '#64748b' }}>{String.fromCharCode(65 + i)}</span>
                      <input 
                        type="text" 
                        value={opt} 
                        onChange={(e) => onOptionChange(i, e.target.value)}
                        placeholder={`Tùy chọn ${i + 1}`}
                        style={{ pointerEvents: 'all', flex: 1, padding: '8px', borderRadius: '6px', border: '1px solid #e2e8f0', fontSize: '14px' }}
                      />
                    </div>
                  ))}
                  <button 
                    onClick={startVoting}
                    style={{ pointerEvents: 'all', marginTop: 'auto', padding: '12px', background: '#10b981', color: '#fff', border: 'none', borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer', fontSize: '16px' }}
                  >
                    🚀 Bắt Đầu Bình Chọn
                  </button>
                </>
              )}

              {status === 'voting' && (
                <>
                  <div style={{ fontSize: '18px', fontWeight: 'bold', color: '#334155', textAlign: 'center', marginBottom: '10px' }}>{question}</div>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', flex: 1 }}>
                    {options.map((opt, i) => {
                      const colors = ['#ef4444', '#3b82f6', '#eab308', '#22c55e'];
                      return (
                        <button 
                          key={i} 
                          onClick={() => handleVoteClick(i)}
                          style={{ pointerEvents: 'all', background: colors[i % 4], color: '#fff', border: 'none', borderRadius: '8px', padding: '15px 10px', fontSize: '15px', fontWeight: 'bold', cursor: 'pointer', boxShadow: '0 4px 6px rgba(0,0,0,0.1)', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', transition: 'transform 0.1s' }}
                        >
                          <span style={{ fontSize: '24px' }}>{String.fromCharCode(65 + i)}</span>
                          <span style={{ fontSize: '12px', marginTop: '5px' }}>{opt}</span>
                        </button>
                      )
                    })}
                  </div>
                  <button 
                    onClick={stopVoting}
                    style={{ pointerEvents: 'all', padding: '10px', background: '#ef4444', color: '#fff', border: 'none', borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer', marginTop: '10px' }}
                  >
                    ⏹ Kết Thúc
                  </button>
                </>
              )}

              {status === 'finished' && (
                <>
                  <div style={{ fontSize: '16px', fontWeight: 'bold', color: '#334155', textAlign: 'center', marginBottom: '15px' }}>{question}</div>
                  <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '10px', justifyContent: 'center' }}>
                    {options.map((opt, i) => {
                      const count = votes[i] || 0;
                      const totalVotes = Object.values(votes).reduce((a, b) => a + b, 0);
                      const percent = totalVotes === 0 ? 0 : Math.round((count / totalVotes) * 100);
                      const colors = ['#ef4444', '#3b82f6', '#eab308', '#22c55e'];
                      return (
                        <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                          <span style={{ fontWeight: 'bold', width: '20px' }}>{String.fromCharCode(65 + i)}</span>
                          <div style={{ flex: 1, height: '24px', background: '#e2e8f0', borderRadius: '12px', overflow: 'hidden', position: 'relative' }}>
                            <div style={{ width: `${percent}%`, height: '100%', background: colors[i % 4], transition: 'width 0.5s ease-out' }}></div>
                          </div>
                          <span style={{ fontWeight: 'bold', color: '#64748b', width: '40px', textAlign: 'right' }}>{count}</span>
                        </div>
                      )
                    })}
                  </div>
                  <button 
                    onClick={() => this.editor.updateShape({ id: shape.id, type: 'quiz', props: { status: 'editing' } })}
                    style={{ pointerEvents: 'all', padding: '8px', background: '#64748b', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', marginTop: 'auto' }}
                  >
                    Chỉnh sửa lại
                  </button>
                </>
              )}
            </div>
          </div>
        </HTMLContainer>
      )
    } catch (err) {
      return <HTMLContainer id={shape.id}>Lỗi QuizShape: {err.message}</HTMLContainer>
    }
  }
}

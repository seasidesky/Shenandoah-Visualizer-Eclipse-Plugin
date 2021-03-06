package org.openjdk.jmc.ext.shenandoahvisualizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Color;

import java.util.BitSet;
import java.util.EnumSet;

import static org.openjdk.jmc.ext.shenandoahvisualizer.Colors.*;

public class RegionStat {

    private static final int PERCENT_MASK = 0x7f;
    private static final int FLAGS_MASK   = 0x3f;

    private static final int USED_SHIFT   = 0;
    private static final int LIVE_SHIFT   = 7;
    private static final int TLAB_SHIFT   = 14;
    private static final int GCLAB_SHIFT  = 21;
    private static final int SHARED_SHIFT = 28;
    private static final int FLAGS_SHIFT  = 58;

    private final RegionState state;
    private final BitSet incoming;
    private final float liveLvl;
    private final float usedLvl;
    private final float tlabLvl;
    private final float gclabLvl;
    private final float sharedLvl;

    public RegionStat(float usedLvl, float liveLvl, float tlabLvl, float gclabLvl, float sharedLvl, RegionState state) {
        this.incoming = null;
        this.usedLvl = usedLvl;
        this.liveLvl = liveLvl;
        this.tlabLvl = tlabLvl;
        this.gclabLvl = gclabLvl;
        this.sharedLvl = sharedLvl;
        this.state = state;
    }


    public RegionStat(long data, String matrix) {
        usedLvl  = ((data >>> USED_SHIFT)  & PERCENT_MASK) / 100F;
        liveLvl  = ((data >>> LIVE_SHIFT)  & PERCENT_MASK) / 100F;
        tlabLvl  = ((data >>> TLAB_SHIFT)  & PERCENT_MASK) / 100F;
        gclabLvl = ((data >>> GCLAB_SHIFT) & PERCENT_MASK) / 100F;
        sharedLvl = ((data >>> SHARED_SHIFT) & PERCENT_MASK) / 100F;

        long stat = (data >>> FLAGS_SHIFT) & FLAGS_MASK;
        state = RegionState.fromOrdinal((int) stat);

        if (!matrix.isEmpty()) {
            this.incoming = new BitSet();
            int idx = 0;
            for (char c : matrix.toCharArray()) {
                c = (char) (c - 32);
                incoming.set(idx++, (c & (1 << 0)) > 0);
                incoming.set(idx++, (c & (1 << 1)) > 0);
                incoming.set(idx++, (c & (1 << 2)) > 0);
                incoming.set(idx++, (c & (1 << 3)) > 0);
                incoming.set(idx++, (c & (1 << 4)) > 0);
                incoming.set(idx++, (c & (1 << 5)) > 0);
            }
        } else {
            this.incoming = null;
        }
    }

    private org.eclipse.swt.graphics.Color selectLive(RegionState s) {
        switch (s) {
            case CSET:
                return LIVE_CSET;
            case HUMONGOUS:
                return LIVE_HUMONGOUS;
            case PINNED_HUMONGOUS:
                return LIVE_PINNED_HUMONGOUS;
            case REGULAR:
                return LIVE_REGULAR;
            case TRASH:
                return LIVE_TRASH;
            case PINNED:
                return LIVE_PINNED;
            case PINNED_CSET:
                return LIVE_PINNED_CSET;
            case EMPTY_COMMITTED:
            case EMPTY_UNCOMMITTED:
                return LIVE_EMPTY;
            default:
                return DEFAULT;
        }
    }

	public void render(GC g, int x, int y, int width, int height) {
		g.setBackground(g.getDevice().getSystemColor(SWT.COLOR_WHITE));
		g.fillRectangle(x, y, width, height);

		switch (state) {
			case REGULAR: {
				if (gclabLvl > 0 || tlabLvl > 0 || sharedLvl > 0) {
			
					int sharedWidth = (int) (width * sharedLvl);
					int tlabWidth = (int) (width * tlabLvl);
					int gclabWidth = (int) (width * gclabLvl);

					int h = height;
					int ly = y + (height - h);
					int lx = x;
					int alpha = liveLvl < 0.5f ? 100 : 255;
					g.setAlpha(alpha);
					
					if (tlabWidth > 0) {
						g.setBackground(TLAB_ALLOC);
						g.fillRectangle(lx, ly, tlabWidth, h);
						g.setForeground(TLAB_ALLOC_BORDER);
						g.drawRectangle(lx, ly, tlabWidth, h);
						lx += tlabWidth;
					}
					
					
					if (gclabWidth > 0) {
						g.setBackground(GCLAB_ALLOC);
						g.fillRectangle(lx, ly, gclabWidth, h);
						g.setForeground(GCLAB_ALLOC_BORDER);
						g.drawRectangle(lx, ly, gclabWidth, h);
						lx += gclabWidth;
					}
					
					if (sharedWidth > 0) {
						
						g.setBackground(SHARED_ALLOC);
						g.fillRectangle(lx, ly, sharedWidth, h);
						g.setForeground(SHARED_ALLOC_BORDER);
						g.drawRectangle(lx, ly, sharedWidth, h);
					}
					g.setAlpha(255);
				}
				break;
			}
			case PINNED: {
				g.setAlpha(255);
				int usedWidth = (int) (width * usedLvl);
				g.setBackground(LIVE_PINNED);
				g.fillRectangle(x, y, usedWidth, height);
				break;
			}
			case CSET:
			case PINNED_CSET:
			case HUMONGOUS:
			case PINNED_HUMONGOUS: {
				g.setAlpha(255);
				int usedWidth = (int) (width * usedLvl);
				g.setBackground(USED);
				g.fillRectangle(x, y, usedWidth, height);

				int liveWidth = (int) (width * liveLvl);
				g.setBackground(selectLive(state));
				g.fillRectangle(x, y, liveWidth, height);

				g.setForeground(LIVE_BORDER);
				g.drawLine(x + liveWidth, y, x + liveWidth, y + height);
				break;
			}
			case EMPTY_COMMITTED: {
				break;
			}
			case EMPTY_UNCOMMITTED: {
				g.setBackground(LIVE_COMMITTED);
				g.fillRectangle(x,y,width,height);
				break;
			}
			case TRASH:{
				g.setForeground(g.getDevice().getSystemColor(SWT.COLOR_BLACK));
				g.drawLine(x, y, x + width, y + height);
				g.drawLine(x, y + height, x + width, y);
				break;
			}	
			default:
				throw new IllegalStateException("Unhandled region state: " + state);
		}

		g.setForeground(Colors.BORDER);
		g.drawRectangle(x, y, width, height);
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegionStat that = (RegionStat) o;

        if (Float.compare(that.liveLvl, liveLvl) != 0) return false;
        if (Float.compare(that.usedLvl, usedLvl) != 0) return false;
        if (Float.compare(that.tlabLvl, tlabLvl) != 0) return false;
        if (Float.compare(that.gclabLvl, gclabLvl) != 0) return false;
        if (Float.compare(that.sharedLvl, sharedLvl) != 0) return false;
        if (!state.equals(that.state)) return false;
        return incoming != null ? incoming.equals(that.incoming) : that.incoming == null;
    }

    @Override
    public int hashCode() {
        int result = state.hashCode();
        result = 31 * result + (incoming != null ? incoming.hashCode() : 0);
        result = 31 * result + (liveLvl != +0.0f ? Float.floatToIntBits(liveLvl) : 0);
        result = 31 * result + (usedLvl != +0.0f ? Float.floatToIntBits(usedLvl) : 0);
        result = 31 * result + (tlabLvl != +0.0f ? Float.floatToIntBits(tlabLvl) : 0);
        result = 31 * result + (gclabLvl != +0.0f ? Float.floatToIntBits(gclabLvl) : 0);
        return result;
    }

    public float live() {
        return liveLvl;
    }

    public float used() {
        return usedLvl;
    }

    public float tlabAllocs() {
        return tlabLvl;
    }

    public float gclabAllocs() {
        return gclabLvl;
    }

    public float sharedAllocs() {
        return sharedLvl;
    }

    public RegionState state() {
        return state;
    }

    public BitSet incoming() {
        return incoming;
    }

}

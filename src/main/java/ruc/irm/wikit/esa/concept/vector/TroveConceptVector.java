package ruc.irm.wikit.esa.concept.vector;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.procedure.TDoubleProcedure;
import gnu.trove.procedure.TIntProcedure;

import java.io.Serializable;

public class TroveConceptVector implements ConceptVector, Serializable {

	private static final long serialVersionUID = 5228670885044409972L;

	private TIntDoubleHashMap valueMap;
	private int size;
	
	public TroveConceptVector(int size ) {
		this.size = size;
		valueMap = new TIntDoubleHashMap(size);		
	}
	
	@Override
	public ConceptVector add(int key, double d) {
		valueMap.put( key, valueMap.get( key ) + d );
        return this;
	}

	@Override
	public ConceptVector add( ConceptVector v ) {
		ConceptIterator it = v.iterator();
		while( it.next() ) {
			add( it.getId(), it.getValue() );
		}
        return this;
	}

    @Override
    public ConceptVector merge( ConceptVector v, double proportion) {
        ConceptIterator it = v.iterator();
        while( it.next() ) {
            double v2 = it.getValue();
            double v1 = this.get(it.getId());
            set(it.getId(), v2 * proportion + v1 * (1 - proportion));
        }
        return this;
    }

	@Override
	public int count() {
		return valueMap.size();
	}

    /**
     * The normalized sum value of all vector items, norm1 equals the sum.
     * @return
     */
    public double getNorm1() {
        NormProcedure n1 = new NormProcedure( 1 );
        valueMap.forEachValue( n1 );
        return n1.getNorm();
    }

    /**
     * Norm2 equals: sqrt(x1^2+x2^2+...+xn^2)
     * @return
     */
    public double getNorm2() {
        NormProcedure n2 = new NormProcedure( 2 );
        valueMap.forEachValue( n2 );
        return n2.getNorm();
    }

	@Override
	public double get( int key ) {
		return valueMap.get( key );
	}

	@Override
	public ConceptIterator iterator() {
		return new TroveConceptVectorIterator();
	}

	@Override
	public ConceptIterator orderedIterator() {
		return new TroveConceptVectorOrderedIterator( valueMap );
	}

	@Override
	public ConceptVector set( int key, double d ) {
		if( d != 0 ) {
			valueMap.put( key, d );
		}
		else {
			valueMap.remove( key );
		}
        return this;
	}

	@Override
	public int size() {
		return size;
	}

    private static class NormProcedure implements TDoubleProcedure {

        int p;
        double sum;

        private NormProcedure( int p ) {
            this.p = p;
            sum = 0;
        }

        @Override
        public boolean execute( double value ) {
            if( p == 1 ) {
                sum += value;
            }
            else {
                sum += Math.pow( value, p );
            }
            return true;
        }

        private double getNorm() {
            return Math.pow( sum, 1.0 / (double)p );
        }
    }

	private class TroveConceptVectorIterator implements ConceptIterator {

		TIntDoubleIterator valueIt;
		
		private TroveConceptVectorIterator() {
			reset();
		}
		
		@Override
		public int getId() {
			return valueIt.key();
		}

		@Override
		public double getValue() {
			return valueIt.value();
		}

		@Override
		public boolean next() {
			if( valueIt.hasNext() ) {
				valueIt.advance();
				return true;
			}
			return false;
		}

		@Override
		public void reset() {
			valueIt = valueMap.iterator();
		}
		
	}

    public static void main(String[] args) {
        TIntDoubleHashMap map = new TIntDoubleHashMap();
        map.put(1, 1.0);
        map.put(2, 2.0);
        map.put(3, 3.0);

        NormProcedure n1 = new NormProcedure( 1 );
        map.forEachValue(n1);
        System.out.println(n1.getNorm());

        NormProcedure n2 = new NormProcedure( 2 );
        map.forEachValue(n2);
        System.out.println(n2.getNorm());
    }
}

package ruc.irm.wikit.esa.concept.vector;


/**
 * The vector for concepts, this vector contains the concept id and value pairs.
 */
public interface ConceptVector {

	public double get(int key);
	
	public ConceptVector add(int key, double d);

	public ConceptVector set(int key, double d);
	
	public ConceptVector add(ConceptVector v);

    /**
     * merge the vector v's value into current vector, calculated by the following formula:
     * old_value*(1-proportion)+new_value*proportion
     * @param v
     * @param proportion
     * @return
     */
    public ConceptVector merge(ConceptVector v, double proportion);
	
	public ConceptIterator iterator();
	
	//public ConceptVectorData getData();

    /**
     * The normalized sum value of all vector items, norm1 equals the sum.
     * @return
     */
    public double getNorm1();

    /**
     * Norm2 equals: sqrt(x1^2+x2^2+...+xn^2)
     * @return
     */
    public double getNorm2();

    public int size();

    /** The total number of concepts in this vector */
	public int count();
	
	public ConceptIterator orderedIterator();
}

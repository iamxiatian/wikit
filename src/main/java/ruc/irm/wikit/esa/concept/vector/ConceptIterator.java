package ruc.irm.wikit.esa.concept.vector;


public interface ConceptIterator {

	public boolean next();

    /**
     * return concept id
     * @return
     */
	public int getId();

    /**
     * return concept value
     * @return
     */
	public double getValue();
	
	public void reset();
	
}

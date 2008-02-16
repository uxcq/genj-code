/**
 * Reports are Freeware Code Snippets
 *
 * This report is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package tree.output;

import java.awt.Graphics2D;

import tree.FamBox;
import tree.IndiBox;

/**
 * Rotates information boxes, preserving their position.
 *
 * @author Przemek Wiech <pwiech@losthive.org>
 */
public class RotateTreeElements extends FilterTreeElements {

    public static final int ROTATE_0 = 0; // No transformation
    public static final int ROTATE_270 = 1;
    public static final int ROTATE_180 = 2;
    public static final int ROTATE_90 = 3;

    private int rotation;

    /**
     * Constructs the object.
     */
    public RotateTreeElements(Graphics2D graphics, TreeElements elements, int rotation) {
        super(graphics, elements);
        this.rotation = rotation;
    }

    public RotateTreeElements(TreeElements elements, int rotation)
    {
        this(null, elements, rotation);
    }

    /**
     * Outputs a rotated image of an individual box.
     * @param i  individual
     * @param x  x coordinate
     * @param y  y coordinate
     * @param gen generation number
     */
    public void drawIndiBox(IndiBox indibox, int x, int y, int gen) {
        transform(x, y, indibox.width, indibox.height);
        transpose(indibox);
        elements.drawIndiBox(indibox, 0, 0, gen);
        transpose(indibox);
        transformBack(x, y, indibox.width, indibox.height);
    }

    /**
     * Outputs a rotated image of a family box.
     * @param i  individual
     * @param x  x coordinate
     * @param y  y coordinate
     * @param gen generation number
     */
    public void drawFamBox(FamBox fambox, int x, int y, int gen) {
        transform(x, y, fambox.width, fambox.height);
        transpose(fambox);
        elements.drawFamBox(fambox, 0, 0, gen);
        transpose(fambox);
        transformBack(x, y, fambox.width, fambox.height);
    }

	/**
     * Applies the rotation transformation.
	 */
    private void transform(int x, int y, int w, int h) {
        switch (rotation) {
            case ROTATE_0:
                graphics.translate(x, y);
                break;
            case ROTATE_90:
                graphics.translate(x, y + h);
                graphics.rotate(-Math.PI/2);
                break;
            case ROTATE_180:
                graphics.translate(x + w, y + h);
                graphics.rotate(Math.PI);
                break;
            case ROTATE_270:
                graphics.translate(x + w, y);
                graphics.rotate(Math.PI/2);
                break;
        }
    }

    /**
     * Takes back the rotation transformation.
     */
    private void transformBack(int x, int y, int w, int h) {
        switch (rotation) {
            case ROTATE_0:
                graphics.translate(-x, -y);
                break;
            case ROTATE_90:
                graphics.rotate(Math.PI/2);
                graphics.translate(-x, -y - h);
                break;
            case ROTATE_180:
                graphics.rotate(-Math.PI);
                graphics.translate(-x - w, -y - h);
                break;
            case ROTATE_270:
                graphics.rotate(-Math.PI/2);
                graphics.translate(-x - w, -y);
                break;
        }
    }

    private void transpose(IndiBox indibox) {
        if (rotation == ROTATE_0 || rotation == ROTATE_180)
            return;
        int tmp = indibox.width;
        indibox.width = indibox.height;
        indibox.height = tmp;
    }

    private void transpose(FamBox fambox) {
        if (rotation == ROTATE_0 || rotation == ROTATE_180)
            return;
        int tmp = fambox.width;
        fambox.width = fambox.height;
        fambox.height = tmp;
    }

    public void getIndiBoxSize(IndiBox indibox)
    {
        elements.getIndiBoxSize(indibox);
        transpose(indibox);
    }

    public void getFamBoxSize(FamBox fambox)
    {
        elements.getFamBoxSize(fambox);
        transpose(fambox);
    }
}

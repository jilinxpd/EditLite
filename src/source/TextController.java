
package source;

/**
 *
 * @author xpd
 * 被Text类和Note类继承
 */
public interface TextController {

    public int next();

    public int last();

    public boolean hasTextJumped();//对Note来说没实际意义
}

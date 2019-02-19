package ut.com.wcg.bitbucket;

import org.junit.Test;
import com.wcg.bitbucket.api.MyPluginComponent;
import com.wcg.bitbucket.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}
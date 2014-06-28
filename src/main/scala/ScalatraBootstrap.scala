import org.joshjoyce.lumivore.web.Lumivore
import org.scalatra.LifeCycle
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context mount(new Lumivore, "/*")
  }
}
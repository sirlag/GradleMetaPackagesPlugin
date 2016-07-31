import com.github.sirlag.GradleMetaDependency
import com.github.sirlag.GradleMetaPackagesPlugin
import com.github.sirlag.GradleMetaRepository
import com.github.sirlag.NoMetaDependencyFoundException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import static org.junit.Assert.*

class GradleMetaPackagesPluginTest {
    @Test
    public void gradleMetaPackagesAddsMetaDependantsTaskToProject(){
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply('com.github.sirlag.GradleMetaPackages')
        assertTrue(project.tasks.metaDependents instanceof Task)
    }

    @Test(expected = NoMetaDependencyFoundException.class)
    public void getMetaDependencyDependenciesTest() throws Exception {
        def fakeDependency = new GradleMetaDependency("fakeDependency")
        def fakeRepo = [new GradleMetaRepository("http://sirlag.me")].toList()

        GradleMetaPackagesPlugin.getMetaDependencyDependencies(fakeDependency, fakeRepo)
    }

    @Test
    public void repoHasMetaDependencyTestTrue(){
        //We are using google for this rather than an actual MetaDependency repo because at this time, no MetaDependency
        //repositories exist.
        //TODO make sure this ends up pointing at a real MetaDependency Repo
        assertTrue(GradleMetaPackagesPlugin.repoHasMetaDependency("https://www.google.com"))
    }

    @Test
    public void repoHasMetaDependencyTestFalse(){
        assertFalse(GradleMetaPackagesPlugin.repoHasMetaDependency("http://sirlag.me/fakepackage/9001.69"))
    }
}

package com.github.sirlag

import org.gradle.api.Plugin
import org.gradle.api.Project


class GradleMetaPackagesPlugin implements Plugin<Project> {
    void apply(Project project){
        def metaDependencies = project.container(GradleMetaDependency)
        project.extensions.metaDependencies = metaDependencies
        def metaRepositories = project.container(GradleMetaRepository)
        project.extensions.metaRepositories = metaRepositories
        project.task("metaDependents") << {
            for(metaDependency in project.extensions.metaDependencies.toList()){
                def dependencies = getMetaDependencyDependencies(metaDependency, project.extensions.metaRepositories.toList())
                dependencies.forEach({project.dependencies.add((String)it[0], it[1])})
            }
        }
    }

    static List<Tuple> getMetaDependencyDependencies(GradleMetaDependency metaDependency, List<GradleMetaRepository> repositories) {

        def optionalRepo = repositories.stream()
            .map({
                return it.location.toString() + "/" + metaDependency.identifier + "/" + metaDependency.version
            }).
            filter({
                repoHasMetaDependency(it.toString())
            }).findFirst()

        if(!optionalRepo.isPresent())
            throw new NoMetaDependencyFoundException("${metaDependency.identifier} was not found.")

        def repo = optionalRepo.get()

        def results = new ArrayList()

        repo.toURL().eachLine {
            def dependency = readMetaLine(it)
            if (dependency!=null)
                results.add(dependency)
            else
                null
        }

        return results
    }

    static Boolean repoHasMetaDependency(String url){
        try {
            HttpURLConnection.setFollowRedirects(false)
            HttpURLConnection con = (HttpURLConnection) url.toURL().openConnection()
            con.setRequestMethod("HEAD")
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK)
        } catch (Exception ignored){
            return false
        }
    }

    static String[] readMetaLine(String line){
        if (!line.startsWith("//")){
            return line.replaceAll("\"|\'", "").split(" ")
        }
        return null
    }

}

class GradleMetaDependency{
    final String identifier
    String version = "latest"

    GradleMetaDependency(String name) {
        this.identifier = name
    }
}

class GradleMetaRepository{
    final URL location
    GradleMetaRepository(String location){
        this.location = location.toURL()
    }
}

class NoMetaDependencyFoundException extends Exception{
    public NoMetaDependencyFoundException(String s){
        super(s)
    }
}


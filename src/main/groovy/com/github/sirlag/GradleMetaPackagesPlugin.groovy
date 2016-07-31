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

    static List<Tuple> getMetaDependencyDependencies(GradleMetaDependency dependency, List repositories) {

        def repo = repositories.stream()
                .map({
            return it.toString() + "/" + dependency.identifier.toString() + "/" + dependency.version.toString()
        }).
                filter({
                    repoHasMetaDependency((String)it)
                }).findFirst().get()

        if(repo == null)
            throw new NoMetaDependencyFoundException("${dependency.identifier} was not found.")
        def results = new ArrayList()

        new URL((String)repo).eachLine {
            if (!it.startsWith("//")){
                def line = it.replaceAll("\"|\'", "").split(" ")
                results.add( new Tuple(line[0], line[1]))
            }
        }

        results

    }

    static Boolean repoHasMetaDependency(String url){
        try {
            HttpURLConnection.setFollowRedirects(false)
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection()
            con.setRequestMethod("HEAD")
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK)
        } catch (Exception ignored){
            return false
        }
    }

}

class GradleMetaDependency{
    final String identifier
    final String version = "latest"

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


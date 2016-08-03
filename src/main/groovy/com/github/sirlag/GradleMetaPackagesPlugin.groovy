package com.github.sirlag

import groovy.json.JsonSlurper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies

class GradleMetaPackagesPlugin implements Plugin<Project> {
    def compileDeps

    void apply(Project project){
        def metaDependenciesContainer = project.container(GradleMetaDependency)
        project.extensions.add("metaDependencies", metaDependenciesContainer)
        def metaRepositoriesContainer = project.container(GradleMetaRepository)
        project.extensions.add("metaRepositories", metaRepositoriesContainer)

        compileDeps = project.getConfigurations().getByName("compile").getDependencies()
        project.getGradle().addListener(new DependencyResolutionListener() {
            @Override
            void beforeResolve(ResolvableDependencies resolvableDependencies) {

                for(metaDependency in project.extensions.metaDependencies.toList()){
                    def dependencies = getMetaDependencyDependencies(metaDependency, project.extensions.metaRepositories.toList())
                    dependencies.forEach{compileDeps.add(project.getDependencies().create((String)it[1]))}
                }
                project.getGradle().removeListener(this)
            }

            @Override
            void afterResolve(ResolvableDependencies resolvableDependencies) {}
        })
    }

    static List<Tuple> getMetaDependencyDependencies(GradleMetaDependency metaDependency, List<GradleMetaRepository> repositories) {

        def optionalRepo = repositories.stream()
            .map{
                return it.location.toString() + "/" + metaDependency.identifier + "/" + metaDependency.version
            }.
            filter{
                repoHasMetaDependency(it.toString())
            }.findFirst()

        if(!optionalRepo.isPresent())
            throw new NoMetaDependencyFoundException("${metaDependency.identifier} was not found.")

        def repo = optionalRepo.get()

        def results = new ArrayList()

        def slurper = new JsonSlurper()
        def jsonResults = slurper.parseText(repo.toURL().readLines().first())

        ((List<String>)jsonResults.dependencies).forEach({
            results.add(readMetaLine(it))
        })

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

    static String[] readMetaLine(String line) {
        return line.replaceAll("\"|\'", "").split(" ")
    }
}

class GradleMetaDependency{
    final String name
    final String identifier
    String version = "latest"

    GradleMetaDependency(String name) {
        this.name = name
        this.identifier = name
    }
}

class GradleMetaRepository{
    final String name
    URL location

    GradleMetaRepository(String name){
        this.name = name
    }
}

class NoMetaDependencyFoundException extends Exception{
    public NoMetaDependencyFoundException(String s){
        super(s)
    }
}


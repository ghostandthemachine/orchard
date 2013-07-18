APP_NAME    = "thinker"
APP_SOURCES = "package.json public/index.html js css"

VOICE_RATE = 280

def node_webkit_path
    return "/Applications/Thinker.app/Contents/MacOS/node-webkit"
end

def node_running?
    return `pgrep node`.split("\n").size > 1
end

def node_pid
    return `pgrep node`.split("\n").first.to_i
end

def kill_node
    Process.kill('INT', node_pid()) if node_running?
end

def start_node
    puts "starting node server"
    # system "say -r #{VOICE_RATE} -v Victoria \"starting node server\""
    system "node app.js &"
end

def start_app
    puts "starting app"
    # system "say -r #{VOICE_RATE} -v Victoria \"initializing application\""
    system "#{node_webkit_path} #{Dir.pwd} $@"
end

def start_testing_app
    puts "starting testing app"
    system "#{node_webkit_path} #{Dir.pwd} $@"
end

def start_cljsbuild
    puts "starting cljsbuild"
    system "lein cljsbuild auto &"
end

def clean
    system "lein cljsbuild clean"
end

def start_repl
    system %{rlwrap -r -m '\"' -b "(){}[],^%3@\";:'" lein trampoline cljsbuild repl-listen}
end

task :repl do
    start_repl
end


task :kill_node do
  kill_node
end


task :run do
  kill_node
  start_node
  start_app
end

task :run_testing do
  kill_node
  start_node
  start_testing_app
end


task :dev do
  unless node_running?
      start_node
  end

  # clean
  start_cljsbuild
  kill_node
  start_app
end


task :deploy do
  system "zip -r #{APP_NAME} #{APP_SOURCES}"
end


#task :default do
#    Rake.application.options.show_task_pattern = //
#    Rake.application.display_tasks_and_comments()
#end

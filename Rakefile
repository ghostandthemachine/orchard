APP_NAME    = "thinker"
APP_SOURCES = "package.json public/index.html js css"


def node_webkit_path
  return "/Applications/node-webkit.app/Contents/MacOS/node-webkit"
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
  system "node app.js &"
end

def start_app
  puts "starting app"
  system "#{node_webkit_path} #{Dir.pwd} $@"
end

def start_cljsbuild
  puts "starting cljsbuild"
  system "lein cljsbuild auto &"
end

def clean
   system "lein cljsbuild clean"
end

def repl
    system "lein trampoline cljsbuild repl-listen"
end


task :kill_node do
  kill_node
end


task :run do
  unless node_running?
    puts "starting node server"
    start_node
  end
  start_app
end


task :run_and_build do
  unless node_running?
    puts "starting node server"
    start_node
  end

  clean
  start_cljsbuild
  start_app
end


task :deploy
  system "zip -r #{APP_NAME} #{APP_SOURCES}"
end



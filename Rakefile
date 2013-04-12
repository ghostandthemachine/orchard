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
  # system "say -v Victoria \"starting node server\""
  system "node app.js &"
end

def start_app
  # system "say -v Victoria \"initializing application\""
  system "#{node_webkit_path} #{Dir.pwd} $@"
end

def start_cljsbuild
  system "lein cljsbuild auto &"
end

task :kill_node do
  kill_node
end

task :run do
  unless node_running?
    puts "starting node server"
    start_node
  end
  puts "starting app"
  start_app
end


task :run_and_build do
  unless node_running?
    puts "starting node server"
    start_node
  end
  puts "starting cljsbuild"
  start_cljsbuild
  puts "starting app"
  start_app
end
# the-offsite-rule

## API usage

Note that for all the posts, we will eventually have to add the user ID to query params

Create new event:

```
curl -X POST -H 'content-type: application/json' -d '{"name": "late night birthday party",
"time": "2020-06-23T21:00:00.000Z"
}' -v -i 'http://localhost:3449/api/new-event'
```

List all events for user (still will only return user 1)
```
curl -X GET -H 'content-type: application/json' -d '{"event-id": 0, "updates": {"name": "my birthday party", "participants": [{"name": "mk", "postcode": "N4 3LR"}],
"time": "2020-06-23T11:00:00.000Z"
}}' -v -i 'http://localhost:3449/api/events?user-id=1'
```

Get single event:

```
curl -X GET -H 'content-type: application/json' -d '{"event-id": 0, "updates": {"name": "my birthday party", "participants": [{"name": "mk", "postcode": "N4 3LR"}],
"time": "2020-06-23T11:00:00.000Z"
}}' -v -i 'http://localhost:3449/api/event?event-id=0'

```

Update event data:

```
curl -X POST -H 'content-type: application/json' -d '{"event-id": 0, "updates": {"name": "my birthday party", "participants": [{"name": "mk", "postcode": "N4 3LR"}],
"time": "2020-06-23T11:00:00.000Z"
}}' -v -i 'http://localhost:3449/api/save'


```

Get event locations (this one is slow as it may have to run the event)

```
curl -X GET -v -i 'http://localhost:3449/api/locations?event-id=0'
```
This is the the-offsite-rule project.

## Development mode

To start the Figwheel compiler, navigate to the project folder and run the following command in the terminal:

```
lein figwheel
```

Figwheel will automatically push cljs changes to the browser. The server will be available at [http://localhost:3449](http://localhost:3449) once Figwheel starts up. 

Figwheel also starts `nREPL` using the value of the `:nrepl-port` in the `:figwheel`
config found in `project.clj`. By default the port is set to `7002`.

The figwheel server can have unexpected behaviors in some situations such as when using
websockets. In this case it's recommended to run a standalone instance of a web server as follows:

```
lein do clean, run
```

The application will now be available at [http://localhost:3000](http://localhost:3000).


### Optional development tools

Start the browser REPL:

```
$ lein repl
```
The Jetty server can be started by running:

```clojure
(start-server)
```
and stopped by running:
```clojure
(stop-server)
```


## Building for release

```
lein do clean, uberjar
```

## Deploying to Heroku

Make sure you have [Git](http://git-scm.com/downloads) and [Heroku toolbelt](https://toolbelt.heroku.com/) installed, then simply follow the steps below.

Optionally, test that your application runs locally with foreman by running.

```
foreman start
```

Now, you can initialize your git repo and commit your application.

```
git init
git add .
git commit -m "init"
```
create your app on Heroku

```
heroku create
```

optionally, create a database for the application

```
heroku addons:add heroku-postgresql
```

The connection settings can be found at your [Heroku dashboard](https://dashboard.heroku.com/apps/) under the add-ons for the app.

deploy the application

```
git push heroku master
```

Your application should now be deployed to Heroku!
For further instructions see the [official documentation](https://devcenter.heroku.com/articles/clojure).

goog.provide('chartreuse_vortex.jsvortex');
goog.require('goog.object');

//
// javascript code for custom pixi sprites
//

//
// rapidStage is a Stage-like component that will call update()
// on sub-components every frame. It adds in a prop 'perFrameUpdaters'
// that is basically an array of update functions to call every frame.
//
// Use this in place of the normal PIXI Stage and then create your
// sub-components to look for a 'perFrameUpdaters' prop on the stage. Your
// sub-component needs to create an update function and add it to the
// perFrameUpdaters array. Make sure you remove it later if the component
// is unmounted!
//
chartreuse_vortex.jsvortex = {}

chartreuse_vortex.jsvortex.rapidStage = React.createClass({
  componentDidMount: function() {
    var stage = this.refs.stage._displayObject;
    stage.perFrameUpdaters = [];
    
    // set up an event that calls all perFrameUpdaters every frame
    var perframefunc = function (hirestime) {
      var i=0;
      var updaters = stage.perFrameUpdaters;
      for (i=0; i < updaters.length; i++) {
	var updaterfunc = updaters[i];
	updaterfunc(this.props.currentTime);
      }
      this.setState({raqEvent: requestAnimationFrame(perframefunc)});
    }.bind(this);
    this.setState({raqEvent: requestAnimationFrame(perframefunc)});
  },
  shouldComponentUpdate: function(nextProps, nextState) {
    // ignore state changes that are just churn from updating raqEvent
    return (nextProps !== this.props);
  },
  componentWillUnmount: function() {
    // clean up any pending requestAnimationFrame requests
    if (typeof this.state.raqEvent !== 'undefined') {
      cancelAnimationFrame(this.state.raqEvent);
      this.setState({raqEvent: undefined});
    }
  },
  render: function() {
    var augmentedprops = goog.object.clone(this.props);
    augmentedprops.ref = "stage";
    return React.createElement(
      ReactPIXI.Stage,
      augmentedprops,
      this.props.children
    );
  }
});

//
// The VortexSprite is a sprite that takes as "input" some values
// for x0,y0,t0,... etc and can position the sprite correctly by extrapolating
// along the usual curve of a body with constant acceleration
//
var VortexSprite = function(texture) {
  PIXI.Sprite.call(this, texture);

  this.t0 = 0;
  this.x0 = 0;
  this.y0 = 0;
  this.dx0 = 0;
  this.dy0 = 0;
  this.ddx0 = 0;
  this.ddy0 = 0;
  this.attime = 0;
  this.userData = null;
};
VortexSprite.prototype = Object.create(PIXI.Sprite.prototype);
VortexSprite.prototype.constructor = VortexSprite;
VortexSprite.prototype.reinterpolate = function(attime) {
  var dt = attime - this.t0;
  this.attime = attime;
  this.x = this.x0 + this.dx0 * dt + 0.5 * this.ddx0 * dt * dt;
  this.y = this.y0 + this.dy0 * dt + 0.5 * this.ddy0 * dt * dt;
}

chartreuse_vortex.jsvortex.interpolatingSprite = ReactPIXI.CustomPIXIComponent(
  {
    customDisplayObject: function(props) {
      var texture = PIXI.Texture.fromImage(props.image)
      return new VortexSprite(texture);
    },

    customApplyProps: function(displayObject, oldProps, newProps) {
      this.applyDisplayObjectProps(oldProps, newProps);
      // set the current interpolating values
      displayObject.t0 = newProps.t0;
      displayObject.x0 = newProps.x0;
      displayObject.y0 = newProps.y0;
      displayObject.dx0 = newProps.dx0;
      displayObject.dy0 = newProps.dy0;
      displayObject.ddx0 = newProps.ddx0;
      displayObject.ddy0 = newProps.ddy0;
      displayObject.reinterpolate(displayObject.attime);
    },

    // add/remove this sprite to the list of items to update
    // on each render tick

    customDidAttach: function(displayObject) {
      var stageupdatelist = displayObject.stage.perFrameUpdaters;
      var updatefunction = function (time) {
	    displayObject.reinterpolate(time);
      };
      displayObject.userData = updatefunction;
      stageupdatelist.push(updatefunction);
    },

    customWillDetach: function(displayObject) {
      var stageupdatelist = displayObject.stage.perFrameUpdaters;
      var funcindex = stageupdatelist.indexOf(displayObject.userData);
      if (funcindex >= 0) {
	    stageupdatelist.splice(funcindex,1);
      }
    }
  }
);

chartreuse_vortex.jsvortex.userChildDisplayContainer = ReactPIXI.CustomPIXIComponent(
  {
    customDisplayObject: function(props) {
      return new PIXI.DisplayObjectContainer();
    },
    customApplyProps: function(displayObject, oldProps, newProps) {
    },

    // Cheesy override of _updateChildren.
    // Don't try this at home
    updateCustomChildren: function(oldChildren, nextChildren, customUpdater, transaction, context) {
      if (typeof oldChildren === 'undefined') {
        oldChildren = [];
      }
      if (typeof nextChildren === 'undefined') {
        nextChildren = [];
      }
      //console.log("custom update children called");
      var prevChildren = this._renderedChildren;

      // the updater takes old and new 'children' and returns an iterator that
      // produces a sequence of modifications to children.
      var updaterInstance = customUpdater(oldChildren, nextChildren);

      var itervalue = updaterInstance.next();
      while (!itervalue.done) {
        console.log(itervalue.value);
        itervalue = updaterInstance.next();
      }
    },

    mountComponent: function(rootID, transaction, context) {

      var props = this._currentElement.props;
      if (typeof this.customDisplayObject !== "object") {
        console.warn("No customDisplayObject method found for a CustomPIXIComponent");
      }
      this._displayObject = this.customDisplayObject(props);

      this.applyDisplayObjectProps({}, props);
      if (this.customApplyProps) {
        this.customApplyProps(this._displayObject, {}, props);
      }

      this.updateCustomChildren([], props.customChildren, props.customUpdater, transaction, context);

      return this._displayObject;
    },

    receiveComponent: function(nextElement, transaction, context) {
      var newProps = nextElement.props;
      var oldProps = this._currentElement.props;

      if (this.customApplyProps) {
        this.customApplyProps(this._displayObject, oldProps, newProps);
      }
      else {
        this.applyDisplayObjectProps(oldProps, newProps);
      }

      this.updateCustomChildren(oldProps.customChildren, newProps.customChildren, newProps.customUpdater, transaction, context);
      this._currentElement = nextElement;
    },

    // customDidAttach and customWillDetach are invoked by DisplayObjectContainerMixin,
    // which is where the attach/detach actually occurs

    unmountComponent: function() {
      var oldProps = this._currentElement.props;
      this.updateCustomChildren(oldProps.customChildren, [], newProps.customUpdater, transaction, context);
    }
  }
);
const functions = require('firebase-functions');

const admin = require('firebase-admin');
admin.initializeApp();


exports.checkAndUpdateHit = functions.database.ref('/games/{gameId}/hits/{hitsId}')
  .onCreate((snapshot, context) => {
    const hit = snapshot.val();

    const players = admin.database().ref('/games').child(context.params.gameId).child('players')

    return players.once('value', function (snapshot) {

      var hitTo;
      snapshot.forEach(function (childSnapshot) {
        var childKey = childSnapshot.key;
        var childData = childSnapshot.val();

        let health = childData.health || 100;

        if (childKey !== hit.hitBy) {
          hitTo = childKey

          if (hit.type == 0) {
            health = health - 100;
          }
          if (hit.type == 1) {
            health = health - 50;
          }
          snapshot.ref.parent.child('players').child(hitTo).child('health').set(health)
        }

      });

    });



  });
